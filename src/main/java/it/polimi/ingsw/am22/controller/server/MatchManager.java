package it.polimi.ingsw.am22.controller.server;

import it.polimi.ingsw.am22.controller.server.datebases.MatchResultDao;
import it.polimi.ingsw.am22.controller.server.persistence.MatchPersistence;
import it.polimi.ingsw.am22.controller.server.persistence.MatchSnapshot;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.network.protocol.ModelDtoMapper;
import it.polimi.ingsw.am22.network.protocol.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;
import it.polimi.ingsw.am22.network.protocol.message.request.AbandonRecoveredMatchRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ChooseTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.CreateMatchRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.DisconnectPlayerRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ListMatchesRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ReconnectRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.SetExpectedPlayersRequest;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchAbandonedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchesListMessage;
import it.polimi.ingsw.am22.network.server.AsyncClientHandler;
import it.polimi.ingsw.am22.network.server.ClientHandler;
import it.polimi.ingsw.am22.network.server.rmi.RmiClientHandler;
import it.polimi.ingsw.am22.view.server.VirtualView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /**
     * How long a crash-recovered match may stay in the "reconnecting" state
     * before it is discarded automatically. If the players have not all come
     * back (and the game resumed) within this window, the match is deleted and
     * everyone still connected is notified.
     */
    private static final long RECOVERY_TIMEOUT_MINUTES = 30;

    /** Fires the per-match recovery timeouts scheduled at restore time. */
    private final ScheduledExecutorService recoveryTimeoutExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "recovery-timeout");
        t.setDaemon(true);
        return t;
    });

    /** Disk store of in-progress matches, used to survive a server crash. */
    private final MatchPersistence persistence;

    /** Periodically snapshots every running match to disk. */
    private final ScheduledExecutorService persistenceSaver = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "match-persistence");
        t.setDaemon(true);
        return t;
    });

    /**
     * Builds the manager, wiring its dependencies, restoring any matches
     * persisted from a previous run and starting the periodic disk saver.
     */
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
            MatchController restoredController = new MatchController(
                    snapshot.matchId(), game, snapshot.hostNickname(), snapshot.expectedPlayers());
            matchesById.put(snapshot.matchId(), newSession(restoredController));
            maxId = Math.max(maxId, parseMatchIdSeq(snapshot.matchId()));
            scheduleRecoveryTimeout(snapshot.matchId());
            System.out.println("[persistence] Restored suspended match " + snapshot.matchId()
                    + " — awaiting player reconnection (auto-removed after "
                    + RECOVERY_TIMEOUT_MINUTES + " min).");
        }
        matchIdSeq.set(maxId);
    }

    /**
     * Schedules the automatic removal of a still-recovering match after
     * {@link #RECOVERY_TIMEOUT_MINUTES}. If the match has meanwhile resumed
     * (no longer recovering) or already been removed, the task is a no-op, so
     * no explicit cancellation is needed.
     */
    private void scheduleRecoveryTimeout(String matchId) {
        recoveryTimeoutExecutor.schedule(() -> {
            MatchSession session = matchesById.get(matchId);
            if (session == null) return;
            synchronized (session) {
                if (session.isRecovering()) {
                    session.handleRecoveryTimeout();
                }
            }
        }, RECOVERY_TIMEOUT_MINUTES, TimeUnit.MINUTES);
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
        recoveryTimeoutExecutor.shutdown();
    }

    /**
     * Entry point for every incoming client request: dispatches it through the
     * visitor, turning any failure into an {@link ErrorMessage} for the caller.
     *
     * @param request the request to handle (a null request yields an error reply)
     * @param channel the channel the request arrived on / replies are sent to
     */
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
     * Probes every currently-bound RMI client to detect drops the transport
     * cannot signal on its own (no read loop server-side). Snapshots the
     * handlers under each session lock, then pings outside the lock so a
     * slow/dead client cannot freeze other operations on the match.
     * Failures route through {@link RmiClientHandler#probe} →
     * {@link #handleTransportDrop}, identical to a failed outbound send.
     */
    public void probeRmiClients() {
        List<RmiClientHandler> rmiHandlers = new ArrayList<>();
        for (MatchSession session : matchesById.values()) {
            synchronized (session) {
                for (ClientHandler ch : session.snapshotChannels()) {
                    // Channels are wrapped in AsyncClientHandler since the move
                    // to non-blocking sends; unwrap to reach the actual RMI handler.
                    ClientHandler raw = (ch instanceof AsyncClientHandler a) ? a.unwrap() : ch;
                    if (raw instanceof RmiClientHandler rmi) {
                        rmiHandlers.add(rmi);
                    }
                }
            }
        }
        for (RmiClientHandler h : rmiHandlers) {
            h.probe();
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
        @Override public void visit(ChooseTotemRequest request) {
            MatchSession s = requireSession(request.matchId());
            synchronized (s) { s.handleChooseTotem(request, channel); }
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
            MatchSession s = matchesById.get(request.matchId());
            if (s == null) {
                // The suspended match is gone: another player abandoned it while
                // this one was still on the reconnect screen. Tell them the match
                // is over instead of the generic "unknown match" error so the
                // client routes back to the start scene with a clear reason.
                channel.send(new MatchAbandonedMessage(
                        "Match " + request.matchId() + " is no longer available — "
                        + "another player left it. The match is over."));
                return;
            }
            synchronized (s) { s.handleReconnect(request, channel); }
        }
        @Override public void visit(AbandonRecoveredMatchRequest request) {
            MatchSession s = matchesById.get(request.matchId());
            if (s == null) {
                // Already torn down (e.g. two players hit "Back" together): nothing to do.
                return;
            }
            synchronized (s) {
                // Only a suspended match can be abandoned this way; ignore the
                // request for a normally-running match as a safety guard.
                if (s.isRecovering()) {
                    s.handleAbandonRecovered(channel);
                }
            }
        }
    }

    // --- Global requests --------------------------------------------------

    private void handleCreateMatch(CreateMatchRequest request, ClientHandler channel) {
        String hostNickname = requireText(request.hostNickname(), "hostNickname");
        String matchId = nextMatchId();
        MatchSession session = newSession(matchId);
        matchesById.put(matchId, session);
        synchronized (session) {
            try {
                session.handleHostCreateAndSetup(hostNickname, request.expectedPlayers(), channel);
            } catch (RuntimeException e) {
                // Two failure modes, different rollback:
                //  - host couldn't even join: lobby empty, drop the session.
                //  - only setExpectedPlayers failed: host is in lobby, KEEP the match
                //    alive so they can fix it via `players <N>`.
                if (session.getMatchController().getLobbyPlayers().isEmpty()) {
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
                MatchController gc = session.getMatchController();
                // A normally-running match is not listable; a crash-recovered
                // one IS, so reconnecting players can see it with a
                // "reconnecting" status and how many players are still missing.
                if (gc.hasStarted() && !session.isRecovering()) continue;
                boolean recovering = session.isRecovering();
                int currentPlayers = recovering
                        ? session.reconnectedCount()
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

    // --- MatchSession factory methods -------------------------------------

    /** Builds a fresh session for a brand-new match, wired with all manager dependencies. */
    private MatchSession newSession(String matchId) {
        return new MatchSession(matchId, mapper, persistence, matchResultDao,
                endGameCloser, END_GAME_CLOSE_DELAY_MS, removeFromRegistryCallback(matchId));
    }

    /** Builds a session around a controller restored from a disk snapshot. */
    private MatchSession newSession(MatchController restoredController) {
        return new MatchSession(restoredController, mapper, persistence, matchResultDao,
                endGameCloser, END_GAME_CLOSE_DELAY_MS,
                removeFromRegistryCallback(restoredController.getMatchId()));
    }

    /** Callback the session invokes to take itself out of the registry. */
    private Runnable removeFromRegistryCallback(String matchId) {
        return () -> matchesById.remove(matchId);
    }
}
