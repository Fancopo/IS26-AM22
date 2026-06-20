package it.polimi.ingsw.am22.controller.client;

import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchAbandonedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchRecoveringMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchesListMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.TotemSelectionMessage;
import it.polimi.ingsw.am22.network.protocol.dto.TotemSelectionStateDTO;

import java.util.Objects;

/**
 * Pairs a ServerConnection with a VirtualServer and adds a
 * snapshot-replay layer: an internal dispatcher captures every server message,
 * caches the latest lobby/game state, then forwards it to the currently
 * attached view handler.
 *
 * <p>When a new screen registers via {@link #setHandler}, it immediately
 * receives a replay of the latest known state — fixes the race where a
 * message arrives before the view is ready.
 */
public final class ClientSession {

    private final ServerConnection connection;
    private final VirtualServer virtualServer;

    /** Handler of the currently active view (may change on every screen switch). */
    private volatile ServerHandler currentHandler;

    private volatile LobbyStateDTO latestLobbyState;
    private volatile TotemSelectionStateDTO latestTotemSelectionState;
    private volatile GameStateDTO latestGameState;
    private volatile boolean gameStarted;

    /**
     * @param connection the open connection to wrap; its message dispatcher is
     *                   set to this session's internal dispatcher
     */
    public ClientSession(ServerConnection connection) {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.virtualServer = new VirtualServer(connection);
        this.connection.setMessageDispatcher(new InternalDispatcher());
    }

    /** @return the local proxy used to send requests to the server */
    public VirtualServer getVirtualServer() { return virtualServer; }

    /** @return the local player's nickname, or null if not joined yet */
    public String getLocalNickname() { return virtualServer.getNickname(); }

    /** @return the latest cached lobby state, or null if none received yet */
    public LobbyStateDTO getLatestLobbyState() { return latestLobbyState; }

    /** @return the latest cached totem-selection state, or null if none received yet */
    public TotemSelectionStateDTO getLatestTotemSelectionState() { return latestTotemSelectionState; }

    /** @return the latest cached game state, or null if none received yet */
    public GameStateDTO getLatestGameState() { return latestGameState; }

    /** @return whether the match has started */
    public boolean isGameStarted() { return gameStarted; }

    /**
     * Registers the active view handler. If a lobby/game snapshot is already
     * available, replays it to the new handler so the screen starts in sync.
     *
     * @param handler the new view handler, or null to detach
     */
    public void setHandler(ServerHandler handler) {
        this.currentHandler = handler;
        if (handler == null) return;
        if (gameStarted && latestGameState != null) {
            handler.onServerMessage(new GameStateMessage(latestGameState));
        } else if (latestTotemSelectionState != null) {
            handler.onServerMessage(new TotemSelectionMessage(latestTotemSelectionState));
        } else if (latestLobbyState != null) {
            handler.onServerMessage(new LobbyStateMessage(latestLobbyState));
        }
    }

    /**
     * Resets local match state (snapshots + virtual-server binding) but keeps the
     * connection. Used both when the server sends MatchClosedMessage and on
     * voluntary leave; afterwards the session is reusable for list/create/join.
     */
    public void clearLocalMatchState() {
        latestGameState = null;
        latestLobbyState = null;
        latestTotemSelectionState = null;
        gameStarted = false;
        virtualServer.clearMatchBinding();
    }

    /**
     * Closes the session.
     *
     * @param notifyServer when true and a lobby was joined, tells the server we
     *                     are leaving before closing the channel
     */
    public void close(boolean notifyServer) {
        try {
            if (notifyServer && virtualServer.hasJoinedLobby()) {
                virtualServer.disconnect();
            }
        } catch (RuntimeException ignored) {
            // Connection already gone: nothing to notify.
        }
        connection.close();
    }

    private final class InternalDispatcher implements ServerHandler {

        @Override
        public void onServerMessage(ServerMessage message) {
            message.accept(new ServerMessageVisitor() {
                @Override public void visit(MatchesListMessage m) {}
                @Override public void visit(MatchJoinedMessage m) {
                    // Store the matchId locally so subsequent moves are routed to the right match.
                    virtualServer.bindMatch(m.matchId(), m.nickname());
                }
                @Override public void visit(LobbyStateMessage m)  { latestLobbyState = m.lobbyState(); }
                @Override public void visit(TotemSelectionMessage m) { latestTotemSelectionState = m.selectionState(); }
                @Override public void visit(GameStartedMessage m) { gameStarted = true; latestTotemSelectionState = null; latestGameState = m.initialGameState(); }
                @Override public void visit(GameStateMessage m)   { latestGameState = m.gameState(); }
                @Override public void visit(MatchRecoveringMessage m) {
                    // The match is in progress (just paused for reconnection):
                    // keep gameStarted set so a further crash is still treated
                    // as a recoverable mid-match drop.
                    gameStarted = true;
                    latestGameState = m.gameState();
                }
                @Override public void visit(EndGameMessage m)     { latestGameState = m.finalGameState(); }
                @Override public void visit(MatchClosedMessage m) {
                    // Match aborted remotely: clean up local state so the view can
                    // bounce the user back to the initial scene. Channel stays open.
                    clearLocalMatchState();
                }
                @Override public void visit(MatchAbandonedMessage m) {
                    // A player left a suspended match: drop local state; the view
                    // tears the whole session down and returns to the start scene.
                    clearLocalMatchState();
                }
                @Override public void visit(ErrorMessage m) {}
            });
            ServerHandler handler = currentHandler;
            if (handler != null) {
                handler.onServerMessage(message);
            }
        }

        @Override
        public void onConnectionClosed(Throwable cause) {
            ServerHandler handler = currentHandler;
            if (handler != null) {
                handler.onConnectionClosed(cause);
            }
        }
    }
}
