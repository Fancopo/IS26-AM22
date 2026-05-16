package it.polimi.ingsw.am22.controller.server;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.controller.server.persistence.MatchPersistence;
import it.polimi.ingsw.am22.controller.server.persistence.MatchSnapshot;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.protocol.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;
import it.polimi.ingsw.am22.network.protocol.message.request.*;
import it.polimi.ingsw.am22.network.protocol.message.response.*;
import it.polimi.ingsw.am22.view.server.VirtualView;
import it.polimi.ingsw.am22.network.protocol.ModelDtoMapper;
import it.polimi.ingsw.am22.controller.server.datebases.MatchResultDao;
import it.polimi.ingsw.am22.network.server.ClientHandler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side core of the multi-match network layer.
 *
 * <p>Holds a registry of {@link MatchSession}s, one per matchId, each with its own
 * {@link MatchController} and {@link VirtualView}. Game requests carry a matchId and
 * get routed to the matching session; global requests (create/list) are handled here.
 *
 * <p>Dispatch goes through {@link ClientRequestVisitor}: no instanceof, and a new
 * request type is a compile-time obligation.
 *
 * <p>Locking: the registry is a {@link ConcurrentHashMap}, so lookup, insertion
 * and removal of {@link MatchSession}s are thread-safe without an external lock.
 * Mutations inside a single session are serialized by synchronizing on the
 * {@code MatchSession} instance itself (used as its own monitor) — distinct
 * matches can therefore progress in parallel, and a slow client in match A
 * does not block match B.
 */
public class MatchManager {

    private final ModelDtoMapper mapper;
    private final Map<String, MatchSession> matchesById;
    private final AtomicLong matchIdSeq;
    private final MatchResultDao matchResultDao;

    /**
     * Defers channel close after EndGameMessage broadcast: closing immediately can
     * race with the client's read loop and surface an EOFException instead of the
     * winner screen.
     */
    private final ScheduledExecutorService endGameCloser = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "endgame-closer");
        t.setDaemon(true);
        return t;
    });

    private static final long END_GAME_CLOSE_DELAY_MS = 3000;

    /** How often started matches are flushed to disk by the periodic saver. */
    private static final long PERSIST_INTERVAL_SECONDS = 15;

    /** Disk store of in-progress matches, used to survive a server crash. */
    private final MatchPersistence persistence;

    /** Periodically snapshots every running match to disk. */
    private final ScheduledExecutorService persistenceSaver = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "match-persistence");
        t.setDaemon(true);
        return t;
    });

    public MatchManager() {
        this.mapper = new ModelDtoMapper();
        this.matchesById = new ConcurrentHashMap<>();
        this.matchIdSeq = new AtomicLong();
        this.matchResultDao = new MatchResultDao();
        this.persistence = new MatchPersistence();
        restorePersistedMatches();
        persistenceSaver.scheduleAtFixedRate(this::persistAllMatches,
                PERSIST_INTERVAL_SECONDS, PERSIST_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Reloads matches that were in progress when the server last stopped.
     * Each becomes a {@link MatchSession} with no bound channels, waiting for
     * its players to reconnect with their original nicknames. The id sequence
     * is advanced past the restored ids so new matches never collide.
     */
    private void restorePersistedMatches() {
        long maxId = 0;
        for (MatchSnapshot snapshot : persistence.loadAll()) {
            Game game = snapshot.game();
            if (game == null || game.isGameEnded()) {
                persistence.delete(snapshot.matchId());
                continue;
            }
            MatchController controller = new MatchController(
                    snapshot.matchId(), game, snapshot.hostNickname(), snapshot.expectedPlayers());
            matchesById.put(snapshot.matchId(), new MatchSession(controller));
            maxId = Math.max(maxId, parseMatchIdSeq(snapshot.matchId()));
            System.out.println("[persistence] Restored suspended match " + snapshot.matchId()
                    + " — awaiting player reconnection.");
        }
        matchIdSeq.set(maxId);
    }

    /** Extracts the numeric counter from a {@code "M-<n>"} match id; 0 if unparseable. */
    private static long parseMatchIdSeq(String matchId) {
        try {
            return Long.parseLong(matchId.substring(matchId.indexOf('-') + 1));
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /** Flushes a snapshot of every running match to disk (periodic-saver task). */
    private void persistAllMatches() {
        for (MatchSession session : matchesById.values()) {
            synchronized (session) {
                session.persistIfRunning();
            }
        }
    }

    /** Final flush + executor shutdown, invoked from the server's shutdown hook. */
    public void shutdown() {
        persistenceSaver.shutdown();
        persistAllMatches();
        endGameCloser.shutdown();
    }

    public void handleRequest(ClientRequest request, ClientHandler channel) {
        if (request == null) {
            channel.send(new ErrorMessage("Null request."));
            return;
        }
        try {
            request.accept(new Dispatcher(channel));
        } catch (Exception e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? "Unexpected network-server error."
                    : e.getMessage();
            channel.send(new ErrorMessage(message));
        }
    }

    /**
     * Called by the transports when the underlying connection drops without a
     * DisconnectPlayerRequest (closed socket, RMI unreachable). Looks up the
     * (matchId, nickname) binding stored on the channel and delegates to the
     * session's {@link MatchSession#handleDisconnect} with transportDrop=true.
     */
    public void handleTransportDrop(ClientHandler channel) {
        String matchId = channel.getBoundMatchId();
        String nickname = channel.getBoundNickname();
        if (matchId == null || nickname == null || nickname.isBlank()) {
            channel.close();
            return;
        }
        MatchSession session = matchesById.get(matchId);
        if (session == null) {
            channel.close();
            return;
        }
        synchronized (session) {
            session.handleDisconnect(nickname, channel, true);
        }
    }

    private final class Dispatcher implements ClientRequestVisitor {
        private final ClientHandler channel;

        private Dispatcher(ClientHandler channel) {
            this.channel = channel;
        }

        @Override public void visit(CreateMatchRequest request) { handleCreateMatch(request, channel); }
        @Override public void visit(ListMatchesRequest request) { handleListMatches(channel); }
        @Override public void visit(AddPlayerToLobbyRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleAddPlayer(request, channel); }
        }
        @Override public void visit(SetExpectedPlayersRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleSetExpectedPlayers(request, channel); }
        }
        @Override public void visit(RemovePlayerFromLobbyRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleRemoveFromLobby(request, channel); }
        }
        @Override public void visit(PlaceTotemRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handlePlaceTotem(request, channel); }
        }
        @Override public void visit(PickCardsRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handlePickCards(request, channel); }
        }
        @Override public void visit(PickBonusCardRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handlePickBonusCard(request, channel); }
        }
        @Override public void visit(DisconnectPlayerRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleDisconnect(request.nickname(), channel, false); }
        }
        @Override public void visit(ReconnectRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleReconnect(request, channel); }
        }
    }

    // --- Global requests --------------------------------------------------

    private void handleCreateMatch(CreateMatchRequest request, ClientHandler channel) {
        String hostNickname = requireText(request.hostNickname(), "hostNickname");
        String matchId = nextMatchId();
        MatchSession session = new MatchSession(matchId);
        matchesById.put(matchId, session);
        synchronized (session) {
            try {
                session.handleHostCreateAndSetup(hostNickname, request.expectedPlayers(), channel);
            } catch (RuntimeException e) {
                // Two failure modes, different rollback:
                //  - host couldn't even join: lobby empty, drop the session.
                //  - only setExpectedPlayers failed: host is in lobby, KEEP the match
                //    alive so they can fix it via `players <N>`.
                if (session.gameController.getLobbyPlayers().isEmpty()) {
                    matchesById.remove(matchId);
                }
                throw e;
            }
        }
    }

    private void handleListMatches(ClientHandler channel) {
        List<MatchInfoDTO> open = new ArrayList<>();
        for (MatchSession session : matchesById.values()) {
            // Read each session under its own lock so we never observe a torn state
            // (e.g. lobby being mutated by another thread while we size it).
            synchronized (session) {
                MatchController gc = session.gameController;
                // A normally-running match is not listable; a crash-recovered
                // one IS, so reconnecting players can see it with a
                // "reconnecting" status and how many players are still missing.
                if (gc.hasStarted() && !session.recovering) continue;
                boolean recovering = session.recovering;
                int currentPlayers = recovering
                        ? session.reconnectedNicknames.size()
                        : gc.getLobbyPlayers().size();
                int expectedPlayers = recovering
                        ? gc.getGame().getPlayers().size()
                        : gc.getExpectedPlayers();
                open.add(new MatchInfoDTO(
                        gc.getMatchId(),
                        gc.getHostNickname(),
                        expectedPlayers,
                        currentPlayers,
                        gc.hasStarted(),
                        recovering));
            }
        }
        channel.send(new MatchesListMessage(open));
    }

    private MatchSession requireSession(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId is required for this request.");
        }
        MatchSession session = matchesById.get(matchId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown match: " + matchId);
        }
        return session;
    }

    private String nextMatchId() {
        return "M-" + matchIdSeq.incrementAndGet();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }

    /** Per-match state: MatchController + VirtualView, isolated from other matches. */
    private final class MatchSession {

        private final MatchController gameController;
        private final VirtualView virtualView;
        private boolean observerAttached;

        /**
         * True while a crash-recovered match is waiting for its players to
         * reconnect. Moves are rejected and the game stays frozen until every
         * player is back.
         */
        private boolean recovering;

        /** Lower-cased nicknames of players that have reconnected so far. */
        private final Set<String> reconnectedNicknames = new HashSet<>();

        private MatchSession(String matchId) {
            this.gameController = new MatchController(matchId);
            this.virtualView = new VirtualView(mapper);
            this.observerAttached = false;
            this.recovering = false;
        }

        /** Builds a session around a controller restored from a disk snapshot. */
        private MatchSession(MatchController restoredController) {
            this.gameController = restoredController;
            this.virtualView = new VirtualView(mapper);
            this.observerAttached = false;
            // A restored match starts paused, waiting for every player to return.
            this.recovering = true;
        }

        /** Writes a snapshot of this match to disk if it is started and not over. */
        private void persistIfRunning() {
            if (gameController.hasStarted() && !gameController.getGame().isGameEnded()) {
                persistence.save(new MatchSnapshot(
                        gameController.getMatchId(),
                        gameController.getHostNickname(),
                        gameController.getExpectedPlayers(),
                        gameController.getGame()));
            }
        }

        /**
         * Resumes a suspended match for a reconnecting player. The match must
         * be a started game and the nickname must belong to one of its
         * players, otherwise an exception bubbles up as an {@link ErrorMessage}
         * — this is the "nickname does not match" error the client surfaces.
         */
        private void handleReconnect(ReconnectRequest request, ClientHandler channel) {
            if (!gameController.hasStarted()) {
                throw new IllegalStateException(
                        "Match " + gameController.getMatchId() + " cannot be resumed.");
            }
            Player player = findGamePlayer(request.nickname());
            if (player == null) {
                throw new IllegalArgumentException("Nickname '" + request.nickname()
                        + "' does not match any player of the suspended match "
                        + gameController.getMatchId() + ".");
            }
            attachObserverIfNeeded();
            bind(player.getNickname(), channel);
            // No MatchJoinedMessage here: the client already knows matchId and
            // nickname (it kept them across the drop), and sending one would
            // make pre-lobby screens navigate to the lobby by mistake.
            reconnectedNicknames.add(player.getNickname().toLowerCase(Locale.ROOT));

            GameStateDTO state = mapper.toGameState(gameController.getGame());
            int total = gameController.getGame().getPlayers().size();

            if (recovering && reconnectedNicknames.size() >= total) {
                // Every player is back: the match leaves the paused state and
                // resumes — the GameStartedMessage unfreezes all clients.
                recovering = false;
                virtualView.broadcast(new GameStartedMessage(state));
            } else if (recovering) {
                // Still waiting: tell everyone bound how many players are missing
                // so each reconnected client can show the "waiting" state.
                virtualView.broadcast(new MatchRecoveringMessage(
                        state, reconnectedNicknames.size(), total, missingNicknames()));
            } else {
                // Match already running normally (defensive): just resync this client.
                channel.send(new GameStartedMessage(state));
            }
        }

        /** Nicknames of players of the match that have not reconnected yet. */
        private List<String> missingNicknames() {
            List<String> missing = new ArrayList<>();
            for (Player p : gameController.getGame().getPlayers()) {
                if (!reconnectedNicknames.contains(p.getNickname().toLowerCase(Locale.ROOT))) {
                    missing.add(p.getNickname());
                }
            }
            return missing;
        }

        /** Case-insensitive lookup of a player inside the (started) game, or null. */
        private Player findGamePlayer(String nickname) {
            if (nickname == null || nickname.isBlank()) return null;
            String normalized = nickname.strip().toLowerCase(Locale.ROOT);
            for (Player p : gameController.getGame().getPlayers()) {
                if (p.getNickname().strip().toLowerCase(Locale.ROOT).equals(normalized)) {
                    return p;
                }
            }
            return null;
        }

        private void handleAddPlayer(AddPlayerToLobbyRequest request, ClientHandler channel) {
            boolean wasStarted = gameController.hasStarted();
            gameController.addPlayerToLobby(request.nickname());
            bind(request.nickname(), channel);
            channel.send(new MatchJoinedMessage(gameController.getMatchId(), request.nickname()));
            publishStateChange(wasStarted);
        }

        /**
         * Coalesces "host joins + expected players set" into a single broadcast.
         * Without this, the client would receive two LobbyStateMessages back-to-back
         * (expected=0 then the requested value) and render the lobby twice.
         */
        private void handleHostCreateAndSetup(String hostNickname, int expectedPlayers, ClientHandler channel) {
            boolean wasStarted = gameController.hasStarted();
            gameController.addPlayerToLobby(hostNickname);
            bind(hostNickname, channel);
            channel.send(new MatchJoinedMessage(gameController.getMatchId(), hostNickname));
            RuntimeException pendingError = null;
            try {
                gameController.setExpectedPlayers(hostNickname, expectedPlayers);
            } catch (RuntimeException e) {
                pendingError = e;
            }
            publishStateChange(wasStarted);
            if (pendingError != null) {
                throw pendingError;
            }
        }

        private void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientHandler channel) {
            bindIfKnown(request.requesterNickname(), channel);
            boolean wasStarted = gameController.hasStarted();
            gameController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
            publishStateChange(wasStarted);
        }

        private void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientHandler channel) {
            bindIfKnown(request.nickname(), channel);
            gameController.removePlayerFromLobby(request.nickname());
            virtualView.unbind(request.nickname());
            unbindMatch(channel);
            // Voluntary leave: channel stays open so the client can return to the
            // "matches list" scene and list/create/join again on the same connection.
            broadcastLobbyState();
            cleanupIfEmpty();
        }

        /**
         * All three move handlers wrap the controller call in a VirtualView batch:
         * a single move typically triggers 5-6 model notifications (state, active
         * player, era, scoring...) and we want to coalesce them into one broadcast.
         * The try/finally keeps the batch from staying open on exception.
         */
        private void handlePlaceTotem(PlaceTotemRequest request, ClientHandler channel) {
            runMove(request.playerNickname(), channel,
                    () -> gameController.placeTotem(request.playerNickname(), request.offerLetter()));
        }

        private void handlePickCards(PickCardsRequest request, ClientHandler channel) {
            runMove(request.playerNickname(), channel,
                    () -> gameController.pickCards(request.playerNickname(), request.selectedCardIds()));
        }

        private void handlePickBonusCard(PickBonusCardRequest request, ClientHandler channel) {
            runMove(request.playerNickname(), channel,
                    () -> gameController.pickBonusCard(request.playerNickname(), request.bonusCardId()));
        }

        private void runMove(String nickname, ClientHandler channel, Runnable move) {
            if (recovering) {
                throw new IllegalStateException(
                        "The match is paused: waiting for all players to reconnect ("
                        + reconnectedNicknames.size() + "/"
                        + gameController.getGame().getPlayers().size() + ").");
            }
            bindIfKnown(nickname, channel);
            virtualView.beginBatch();
            try {
                move.run();
            } finally {
                virtualView.endBatch();
            }
            broadcastGameStateAndMaybeEnd();
            // Persist right after every move so a crash loses at most the
            // moves made since the last (this) save.
            persistIfRunning();
        }

        /**
         * Handles both a voluntary leave (transportDrop=false) and a transport
         * drop (transportDrop=true). Pre-game: the player just leaves the lobby.
         * Mid-game: the match is aborted for everyone; surviving channels stay
         * connected to the server (unbound) so they can list/create/join again.
         */
        private void handleDisconnect(String nickname, ClientHandler channel, boolean transportDrop) {
            if (!gameController.hasStarted()) {
                try {
                    gameController.removePlayerFromLobby(nickname);
                } catch (Exception ignored) {
                }
                virtualView.unbind(nickname);
                unbindMatch(channel);
                if (transportDrop) channel.close();
                broadcastLobbyState();
                cleanupIfEmpty();
                return;
            }

            // Mid-game: the match is aborted for everyone. A match aborted by a
            // player disconnect is NOT resumable, so its snapshot is discarded —
            // only a server crash leaves a recoverable match behind.
            persistence.delete(gameController.getMatchId());
            virtualView.unbind(nickname);
            unbindMatch(channel);
            if (transportDrop) channel.close();
            virtualView.broadcast(new MatchClosedMessage(
                    "Player " + nickname + " disconnected. The match has been closed."));
            // Detach surviving channels from the match WITHOUT closing them:
            // they stay connected to the server for future list/create/join.
            for (ClientHandler other : virtualView.snapshotChannels()) {
                unbindMatch(other);
            }
            virtualView.unbindAllKeepingChannels();
            matchesById.remove(gameController.getMatchId());
        }

        /**
         * Picks the right message for the transition:
         *   just-started → {@link GameStartedMessage} (carries initial state);
         *   running      → {@link GameStateMessage};
         *   still lobby  → {@link LobbyStateMessage}.
         * On just-started we don't ALSO emit a GameStateMessage: the started
         * message already carries the state, sending both causes a double render.
         */
        private void publishStateChange(boolean wasStarted) {
            if (!wasStarted && gameController.hasStarted()) {
                attachObserverIfNeeded();
                GameStateDTO state = mapper.toGameState(gameController.getGame());
                virtualView.broadcast(new GameStartedMessage(state));
                maybeBroadcastEndGame(state);
                // First disk snapshot of the freshly started match.
                persistIfRunning();
            } else if (gameController.hasStarted()) {
                broadcastGameStateAndMaybeEnd();
            } else {
                broadcastLobbyState();
            }
        }

        private void broadcastLobbyState() {
            virtualView.broadcast(new LobbyStateMessage(mapper.toLobbyState(gameController)));
        }

        /**
         * NB: doesn't broadcast a GameStateMessage. VirtualView is already an
         * observer of Game; the model emits notifyObservers() on every mutation,
         * so an explicit broadcast here would produce a duplicate render.
         */
        private void broadcastGameStateAndMaybeEnd() {
            if (!gameController.hasStarted()) return;
            maybeBroadcastEndGame(mapper.toGameState(gameController.getGame()));
        }

        private void maybeBroadcastEndGame(GameStateDTO state) {
            if (!gameController.getGame().isGameEnded()) return;

            // The match is over: drop its disk snapshot so it is not resurrected
            // as a "suspended match" on the next server start.
            persistence.delete(gameController.getMatchId());

            // determineWinner() calls notifyObservers() in turn: without a batch the
            // observer would re-emit a GameStateMessage identical to the one we just
            // sent at end-of-action, duplicating the Game Over render. We wrap and
            // discard: the final state is already inside EndGameMessage below.
            Player winner;
            virtualView.beginBatch();
            try {
                winner = gameController.determineWinner();
            } finally {
                virtualView.endBatch(false);
            }

            List<Player> players = gameController.getGame().getPlayers();
            int numPlayers = players.size();
            List<MatchResultDao.PlayerResult> results = new ArrayList<>(numPlayers);
            Map<String, Integer> finalScores = new HashMap<>(numPlayers * 2);
            for (Player p : players) {
                int score = p.finalPP();
                results.add(new MatchResultDao.PlayerResult(p.getNickname(), score));
                finalScores.put(p.getNickname(), score);
            }

            List<LeaderboardEntryDTO> leaderboard = List.of();
            Map<String, Integer> positions = Map.of();
            try {
                matchResultDao.saveMatch(results, numPlayers);
                leaderboard = loadLeaderboard(numPlayers);
                positions = computePositions(numPlayers, finalScores);
            } catch (SQLException e) {
                System.err.println("[match " + gameController.getMatchId()
                        + "] Leaderboard persistence unavailable: " + e.getMessage());
            }

            virtualView.broadcast(new EndGameMessage(
                    mapper.toWinnerDTO(winner), state, leaderboard, positions));
            // Drop the match right away so no further requests reach it, but defer
            // the channel close: see endGameCloser.
            matchesById.remove(gameController.getMatchId());
            VirtualView viewToClose = virtualView;
            endGameCloser.schedule(viewToClose::closeAll, END_GAME_CLOSE_DELAY_MS, TimeUnit.MILLISECONDS);
        }

        private List<LeaderboardEntryDTO> loadLeaderboard(int numPlayers) throws SQLException {
            List<MatchResultDao.RankRow> rows = matchResultDao.getLeaderboard(numPlayers);
            List<LeaderboardEntryDTO> out = new ArrayList<>(rows.size());
            for (MatchResultDao.RankRow r : rows) {
                out.add(new LeaderboardEntryDTO(r.nickname(), r.score(), r.endDate(), numPlayers));
            }
            return out;
        }

        private Map<String, Integer> computePositions(int numPlayers,
                                                      Map<String, Integer> finalScores) throws SQLException {
            Map<String, Integer> positions = new HashMap<>(finalScores.size() * 2);
            for (Map.Entry<String, Integer> e : finalScores.entrySet()) {
                positions.put(e.getKey(), matchResultDao.getPosition(numPlayers, e.getValue()));
            }
            return positions;
        }

        private void bind(String nickname, ClientHandler channel) {
            virtualView.bindOrReplace(nickname, channel);
            channel.setBoundMatchId(gameController.getMatchId());
        }

        /** Soft variant of {@link #bind}: re-aligns the channel only if the nickname is already known. */
        private void bindIfKnown(String nickname, ClientHandler channel) {
            if (nickname == null || nickname.isBlank()) return;
            if (virtualView.isBound(nickname)) {
                bind(nickname, channel);
            }
        }

        /** Detach the channel from this match's id (only if it still matches — guards against re-assignment). */
        private void unbindMatch(ClientHandler channel) {
            if (Objects.equals(channel.getBoundMatchId(), gameController.getMatchId())) {
                channel.setBoundMatchId(null);
            }
        }

        private void cleanupIfEmpty() {
            if (!gameController.hasStarted() && gameController.getLobbyPlayers().isEmpty()) {
                matchesById.remove(gameController.getMatchId());
            }
        }

        /**
         * Attaches VirtualView as a Game observer once per session. Without the
         * guard, multiple subscriptions would emit N duplicate messages per
         * model mutation.
         */
        private void attachObserverIfNeeded() {
            if (observerAttached || gameController.getGame() == null) return;
            gameController.getGame().addObserver(virtualView);
            observerAttached = true;
        }
    }
}
