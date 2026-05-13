package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage;

import java.util.Objects;

/**
 * Pairs an ObservableServerConnection with a ClientController and adds a
 * snapshot-replay layer: an internal dispatcher captures every server message,
 * caches the latest lobby/game state, then forwards it to the currently
 * attached view handler.
 *
 * <p>When a new screen registers via {@link #setHandler}, it immediately
 * receives a replay of the latest known state — fixes the race where a
 * message arrives before the view is ready.
 */
public final class ClientSession {

    private final ObservableServerConnection connection;
    private final ClientController clientController;

    /** Handler of the currently active view (may change on every screen switch). */
    private volatile ClientUpdateHandler currentHandler;

    private volatile LobbyStateDTO latestLobbyState;
    private volatile GameStateDTO latestGameState;
    private volatile boolean gameStarted;

    public ClientSession(ObservableServerConnection connection) {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.clientController = new ClientController(connection);
        this.connection.setClientUpdateHandler(new InternalDispatcher());
    }

    public ClientController getClientController() { return clientController; }
    public String getLocalNickname() { return clientController.getNickname(); }
    public LobbyStateDTO getLatestLobbyState() { return latestLobbyState; }
    public GameStateDTO getLatestGameState() { return latestGameState; }
    public boolean isGameStarted() { return gameStarted; }

    /**
     * Registers the active view handler. If a lobby/game snapshot is already
     * available, replays it to the new handler so the screen starts in sync.
     */
    public void setHandler(ClientUpdateHandler handler) {
        this.currentHandler = handler;
        if (handler == null) return;
        if (gameStarted && latestGameState != null) {
            handler.onServerMessage(new GameStateMessage(latestGameState));
        } else if (latestLobbyState != null) {
            handler.onServerMessage(new LobbyStateMessage(latestLobbyState));
        }
    }

    /**
     * Resets local match state (snapshots + controller binding) but keeps the
     * connection. Used both when the server sends MatchClosedMessage and on
     * voluntary leave; afterwards the session is reusable for list/create/join.
     */
    public void clearLocalMatchState() {
        latestGameState = null;
        latestLobbyState = null;
        gameStarted = false;
        clientController.clearMatchBinding();
    }

    public void close(boolean notifyServer) {
        try {
            if (notifyServer && clientController.hasJoinedLobby()) {
                clientController.disconnect();
            }
        } catch (RuntimeException ignored) {
            // Connection already gone: nothing to notify.
        }
        connection.close();
    }

    private final class InternalDispatcher implements ClientUpdateHandler {

        @Override
        public void onServerMessage(ServerMessage message) {
            message.accept(new ServerMessageVisitor() {
                @Override public void visit(MatchesListMessage m) {}
                @Override public void visit(MatchJoinedMessage m) {
                    // Store the matchId locally so subsequent moves are routed to the right match.
                    clientController.bindMatch(m.matchId(), m.nickname());
                }
                @Override public void visit(LobbyStateMessage m)  { latestLobbyState = m.lobbyState(); }
                @Override public void visit(GameStartedMessage m) { gameStarted = true; latestGameState = m.initialGameState(); }
                @Override public void visit(GameStateMessage m)   { latestGameState = m.gameState(); }
                @Override public void visit(EndGameMessage m)     { latestGameState = m.finalGameState(); }
                @Override public void visit(MatchClosedMessage m) {
                    // Match aborted remotely: clean up local state so the view can
                    // bounce the user back to the initial scene. Channel stays open.
                    clearLocalMatchState();
                }
                @Override public void visit(ErrorMessage m) {}
                @Override public void visit(InfoMessage m) {}
            });
            ClientUpdateHandler handler = currentHandler;
            if (handler != null) {
                handler.onServerMessage(message);
            }
        }

        @Override
        public void onConnectionClosed(Throwable cause) {
            ClientUpdateHandler handler = currentHandler;
            if (handler != null) {
                handler.onConnectionClosed(cause);
            }
        }
    }
}
