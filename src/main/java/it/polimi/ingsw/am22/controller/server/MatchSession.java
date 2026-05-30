package it.polimi.ingsw.am22.controller.server;

import it.polimi.ingsw.am22.controller.server.datebases.MatchResultDao;
import it.polimi.ingsw.am22.controller.server.persistence.MatchPersistence;
import it.polimi.ingsw.am22.controller.server.persistence.MatchSnapshot;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.network.protocol.ModelDtoMapper;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.protocol.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ReconnectRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.SetExpectedPlayersRequest;
import it.polimi.ingsw.am22.network.protocol.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchAbandonedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchRecoveringMessage;
import it.polimi.ingsw.am22.network.server.ClientHandler;
import it.polimi.ingsw.am22.view.server.VirtualView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Per-match state: {@link MatchController} + {@link VirtualView}, isolated from
 * other matches. Driven by {@link MatchManager}, which serializes all mutations
 * via {@code synchronized(session)} before invoking the {@code handle*} methods.
 *
 * <p>Dependencies (mapper, persistence, DAO, end-game closer) are injected by
 * the manager. Auto-removal from the manager's registry happens via the
 * {@code removeFromRegistry} callback, so this class never references the
 * manager directly.
 */
public final class MatchSession {

    private final MatchController matchController;
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

    private final ModelDtoMapper mapper;
    private final MatchPersistence persistence;
    private final MatchResultDao matchResultDao;
    private final ScheduledExecutorService endGameCloser;
    private final long endGameCloseDelayMs;
    private final Runnable removeFromRegistry;

    public MatchSession(String matchId,
                        ModelDtoMapper mapper,
                        MatchPersistence persistence,
                        MatchResultDao matchResultDao,
                        ScheduledExecutorService endGameCloser,
                        long endGameCloseDelayMs,
                        Runnable removeFromRegistry) {
        this(new MatchController(matchId), false,
                mapper, persistence, matchResultDao,
                endGameCloser, endGameCloseDelayMs, removeFromRegistry);
    }

    /** Builds a session around a controller restored from a disk snapshot. */
    public MatchSession(MatchController restoredController,
                        ModelDtoMapper mapper,
                        MatchPersistence persistence,
                        MatchResultDao matchResultDao,
                        ScheduledExecutorService endGameCloser,
                        long endGameCloseDelayMs,
                        Runnable removeFromRegistry) {
        // A restored match starts paused, waiting for every player to return.
        this(restoredController, true,
                mapper, persistence, matchResultDao,
                endGameCloser, endGameCloseDelayMs, removeFromRegistry);
    }

    private MatchSession(MatchController matchController,
                         boolean recovering,
                         ModelDtoMapper mapper,
                         MatchPersistence persistence,
                         MatchResultDao matchResultDao,
                         ScheduledExecutorService endGameCloser,
                         long endGameCloseDelayMs,
                         Runnable removeFromRegistry) {
        this.matchController = matchController;
        this.virtualView = new VirtualView(mapper);
        this.observerAttached = false;
        this.recovering = recovering;
        this.mapper = mapper;
        this.persistence = persistence;
        this.matchResultDao = matchResultDao;
        this.endGameCloser = endGameCloser;
        this.endGameCloseDelayMs = endGameCloseDelayMs;
        this.removeFromRegistry = removeFromRegistry;
    }

    // --- Read-only accessors used by MatchManager for listing/lookup --------

    public MatchController getMatchController() { return matchController; }

    /** Snapshot of all currently bound client handlers — used by the RMI liveness probe. */
    public Collection<ClientHandler> snapshotChannels() { return virtualView.snapshotChannels(); }

    public boolean isRecovering() { return recovering; }

    public int reconnectedCount() { return reconnectedNicknames.size(); }

    // --- Persistence (called by MatchManager's periodic saver) --------------

    /** Writes a snapshot of this match to disk if it is started and not over. */
    public void persistIfRunning() {
        if (matchController.hasStarted() && !matchController.getGame().isGameEnded()) {
            persistence.save(new MatchSnapshot(
                    matchController.getMatchId(),
                    matchController.getHostNickname(),
                    matchController.getExpectedPlayers(),
                    matchController.getGame()));
        }
    }

    // --- Request handlers ---------------------------------------------------

    /**
     * Resumes a suspended match for a reconnecting player. The match must
     * be a started game and the nickname must belong to one of its
     * players, otherwise an exception bubbles up as an {@code ErrorMessage}
     * — this is the "nickname does not match" error the client surfaces.
     */
    public void handleReconnect(ReconnectRequest request, ClientHandler channel) {
        // Direct reconnect (transport drop): the client kept matchId/nickname
        // across the drop, so no MatchJoinedMessage is needed.
        handleReconnect(request, channel, false);
    }

    /**
     * @param announceJoin when true, send a {@link MatchJoinedMessage} so the
     *        client binds its local matchId. Needed when reached via the
     *        {@code join <matchId>} command (the client did NOT retain match
     *        state); a true transport-drop reconnect passes false.
     */
    private void handleReconnect(ReconnectRequest request, ClientHandler channel, boolean announceJoin) {
        requireNotInOtherMatch(channel);
        if (!matchController.hasStarted()) {
            throw new IllegalStateException(
                    "Match " + matchController.getMatchId() + " cannot be resumed.");
        }
        Player player = findGamePlayer(request.nickname());
        if (player == null) {
            throw new IllegalArgumentException("Nickname '" + request.nickname()
                    + "' does not match any player of the suspended match "
                    + matchController.getMatchId() + ".");
        }
        // Reject a second client trying to take over a nickname that another
        // client has already reconnected with: one live channel per player.
        if (virtualView.isBound(player.getNickname())) {
            throw new IllegalStateException("Nickname '" + request.nickname()
                    + "' is already connected to match "
                    + matchController.getMatchId() + ".");
        }
        attachObserverIfNeeded();
        bind(player.getNickname(), channel);
        if (announceJoin) {
            // Sent BEFORE the state broadcast below so the client sets its
            // matchId first and can then act on the resumed match.
            channel.send(new MatchJoinedMessage(
                    matchController.getMatchId(), player.getNickname()));
        }
        reconnectedNicknames.add(player.getNickname().toLowerCase(Locale.ROOT));

        GameStateDTO state = mapper.toGameState(matchController.getGame());
        int total = matchController.getGame().getPlayers().size();

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

    /**
     * Tears down a suspended match because a reconnecting player chose "Leave
     * this match" instead of resuming it. The abandoning channel is not bound
     * here (it reconnected only to send this request), so detaching it is a
     * harmless no-op.
     */
    public void handleAbandonRecovered(ClientHandler channel) {
        unbindMatch(channel);
        tearDownRecovered("A player chose to leave the suspended match "
                + matchController.getMatchId() + ". The match is over.");
    }

    /**
     * Tears down a suspended match that nobody finished resuming within the
     * allowed window. Called by the manager's recovery-timeout scheduler while
     * the match is still {@link #recovering}.
     */
    public void handleRecoveryTimeout() {
        tearDownRecovered("The suspended match " + matchController.getMatchId()
                + " was not resumed in time and has been removed.");
    }

    /**
     * Discards a suspended match for good: the snapshot is deleted so it is
     * never resurrected again, and every player that had already reconnected is
     * told the match is over — their channels stay open so each client can pick
     * a new nickname and play again on the same connection.
     */
    private void tearDownRecovered(String reason) {
        persistence.delete(matchController.getMatchId());
        virtualView.broadcast(new MatchAbandonedMessage(reason));
        // Detach every reconnected player from the match WITHOUT closing the
        // channel, mirroring a mid-game abort: they stay connected only long
        // enough to receive the notice and reset themselves.
        for (ClientHandler other : virtualView.snapshotChannels()) {
            unbindMatch(other);
        }
        virtualView.unbindAllKeepingChannels();
        removeFromRegistry.run();
    }

    public void handleAddPlayer(AddPlayerToLobbyRequest request, ClientHandler channel) {
        requireNotInOtherMatch(channel);
        // A crash-recovered match is listed as joinable so its players can come
        // back, but it has already started: a `join` on it is a reconnect, not a
        // fresh lobby join. Route it accordingly so the client isn't rejected
        // with "the match has already started" (handleReconnect surfaces a
        // clearer error if the nickname is not one of the match's players).
        if (recovering) {
            handleReconnect(new ReconnectRequest(matchController.getMatchId(),
                    request.nickname()), channel, true);
            return;
        }
        boolean wasStarted = matchController.hasStarted();
        matchController.addPlayerToLobby(request.nickname());
        bind(request.nickname(), channel);
        channel.send(new MatchJoinedMessage(matchController.getMatchId(), request.nickname()));
        publishStateChange(wasStarted);
    }

    /**
     * Coalesces "host joins + expected players set" into a single broadcast.
     * Without this, the client would receive two LobbyStateMessages back-to-back
     * (expected=0 then the requested value) and render the lobby twice.
     */
    public void handleHostCreateAndSetup(String hostNickname, int expectedPlayers, ClientHandler channel) {
        requireNotInOtherMatch(channel);
        boolean wasStarted = matchController.hasStarted();
        matchController.addPlayerToLobby(hostNickname);
        bind(hostNickname, channel);
        channel.send(new MatchJoinedMessage(matchController.getMatchId(), hostNickname));
        RuntimeException pendingError = null;
        try {
            matchController.setExpectedPlayers(hostNickname, expectedPlayers);
        } catch (RuntimeException e) {
            pendingError = e;
        }
        publishStateChange(wasStarted);
        if (pendingError != null) {
            throw pendingError;
        }
    }

    /**
     * Rejects entering this match from a channel already bound to a different
     * one: a client can be in at most one match at a time. Without this, a
     * player resumed into match A could {@code join}/reconnect to match B on the
     * same connection and end up in two matches at once.
     */
    private void requireNotInOtherMatch(ClientHandler channel) {
        String bound = channel.getBoundMatchId();
        if (bound != null && !bound.equals(matchController.getMatchId())) {
            throw new IllegalStateException("You are already in match " + bound
                    + ". Leave it before joining another match.");
        }
    }

    public void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientHandler channel) {
        bindIfKnown(request.requesterNickname(), channel);
        boolean wasStarted = matchController.hasStarted();
        matchController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
        publishStateChange(wasStarted);
    }

    public void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientHandler channel) {
        bindIfKnown(request.nickname(), channel);
        matchController.removePlayerFromLobby(request.nickname());
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
    public void handlePlaceTotem(PlaceTotemRequest request, ClientHandler channel) {
        runMove(request.playerNickname(), channel,
                () -> matchController.placeTotem(request.playerNickname(), request.offerLetter()));
    }

    public void handlePickCards(PickCardsRequest request, ClientHandler channel) {
        runMove(request.playerNickname(), channel,
                () -> matchController.pickCards(request.playerNickname(), request.selectedCardIds()));
    }

    public void handlePickBonusCard(PickBonusCardRequest request, ClientHandler channel) {
        runMove(request.playerNickname(), channel,
                () -> matchController.pickBonusCard(request.playerNickname(), request.bonusCardId()));
    }

    /**
     * Handles both a voluntary leave (transportDrop=false) and a transport
     * drop (transportDrop=true). Pre-game: the player just leaves the lobby.
     * Mid-game: the match is aborted for everyone; surviving channels stay
     * connected to the server (unbound) so they can list/create/join again.
     */
    public void handleDisconnect(String nickname, ClientHandler channel, boolean transportDrop) {
        if (!matchController.hasStarted()) {
            try {
                matchController.removePlayerFromLobby(nickname);
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
        persistence.delete(matchController.getMatchId());
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
        removeFromRegistry.run();
    }

    // --- Internal helpers ---------------------------------------------------

    private void runMove(String nickname, ClientHandler channel, Runnable move) {
        if (recovering) {
            throw new IllegalStateException(
                    "The match is paused: waiting for all players to reconnect ("
                    + reconnectedNicknames.size() + "/"
                    + matchController.getGame().getPlayers().size() + ").");
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
     * Picks the right message for the transition:
     *   just-started → {@link GameStartedMessage} (carries initial state);
     *   running      → {@code GameStateMessage};
     *   still lobby  → {@link LobbyStateMessage}.
     * On just-started we don't ALSO emit a GameStateMessage: the started
     * message already carries the state, sending both causes a double render.
     */
    private void publishStateChange(boolean wasStarted) {
        if (!wasStarted && matchController.hasStarted()) {
            attachObserverIfNeeded();
            GameStateDTO state = mapper.toGameState(matchController.getGame());
            virtualView.broadcast(new GameStartedMessage(state));
            maybeBroadcastEndGame(state);
            // First disk snapshot of the freshly started match.
            persistIfRunning();
        } else if (matchController.hasStarted()) {
            broadcastGameStateAndMaybeEnd();
        } else {
            broadcastLobbyState();
        }
    }

    private void broadcastLobbyState() {
        virtualView.broadcast(new LobbyStateMessage(mapper.toLobbyState(matchController)));
    }

    /**
     * NB: doesn't broadcast a GameStateMessage. VirtualView is already an
     * observer of Game; the model emits notifyObservers() on every mutation,
     * so an explicit broadcast here would produce a duplicate render.
     */
    private void broadcastGameStateAndMaybeEnd() {
        if (!matchController.hasStarted()) return;
        maybeBroadcastEndGame(mapper.toGameState(matchController.getGame()));
    }

    private void maybeBroadcastEndGame(GameStateDTO state) {
        if (!matchController.getGame().isGameEnded()) return;

        // The match is over: drop its disk snapshot so it is not resurrected
        // as a "suspended match" on the next server start.
        persistence.delete(matchController.getMatchId());

        // determineWinner() calls notifyObservers() in turn: without a batch the
        // observer would re-emit a GameStateMessage identical to the one we just
        // sent at end-of-action, duplicating the Game Over render. We wrap and
        // discard: the final state is already inside EndGameMessage below.
        Player winner;
        virtualView.beginBatch();
        try {
            winner = matchController.determineWinner();
        } finally {
            virtualView.endBatch(false);
        }

        List<Player> players = matchController.getGame().getPlayers();
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
            System.err.println("[match " + matchController.getMatchId()
                    + "] Leaderboard persistence unavailable: " + e.getMessage());
        }

        virtualView.broadcast(new EndGameMessage(
                mapper.toWinnerDTO(winner), state, leaderboard, positions));
        // Drop the match right away so no further requests reach it, but defer
        // the channel close: see endGameCloser.
        removeFromRegistry.run();
        VirtualView viewToClose = virtualView;
        endGameCloser.schedule(viewToClose::closeAll, endGameCloseDelayMs, TimeUnit.MILLISECONDS);
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
        channel.setBoundMatchId(matchController.getMatchId());
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
        if (Objects.equals(channel.getBoundMatchId(), matchController.getMatchId())) {
            channel.setBoundMatchId(null);
        }
    }

    private void cleanupIfEmpty() {
        if (!matchController.hasStarted() && matchController.getLobbyPlayers().isEmpty()) {
            removeFromRegistry.run();
        }
    }

    /**
     * Attaches VirtualView as a Game observer once per session. Without the
     * guard, multiple subscriptions would emit N duplicate messages per
     * model mutation.
     */
    private void attachObserverIfNeeded() {
        if (observerAttached || matchController.getGame() == null) return;
        matchController.getGame().addObserver(virtualView);
        observerAttached = true;
    }

    /** Nicknames of players of the match that have not reconnected yet. */
    private List<String> missingNicknames() {
        List<String> missing = new ArrayList<>();
        for (Player p : matchController.getGame().getPlayers()) {
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
        for (Player p : matchController.getGame().getPlayers()) {
            if (p.getNickname().strip().toLowerCase(Locale.ROOT).equals(normalized)) {
                return p;
            }
        }
        return null;
    }
}
