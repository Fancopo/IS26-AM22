package it.polimi.ingsw.am22.view.gui;

import  it.polimi.ingsw.am22.controller.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;
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

import it.polimi.ingsw.am22.view.gui.screen.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * JavaFX root: owns the {@link Stage} and the {@link ClientSession}, acts as
 * the {@link ServerHandler} (marshalling messages onto the FX thread),
 * and drives screen navigation: Connection → Nickname → Matches → Lobby →
 * Game → EndGame. Each screen's {@code onServerMessage(...)} is always
 * called on the FX thread.
 */
public final class GuiApp extends Application implements ServerHandler {

    private Stage stage;
    private ClientSession session;
    private GuiScreen currentScreen;

    /** Last successful connection parameters, reused after a voluntary leave. */
    private Transport lastTransport;
    private String lastHost;
    private int lastPort;
    /** Last nickname used: needed when bouncing back to the matches screen post-game. */
    private String lastNickname;

    /** When true, the next onConnectionClosed is silent (it was a planned tear-down). */
    private boolean expectingDisconnect;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("MESOS");
        // Window X → clean session shutdown.
        stage.setOnCloseRequest(e -> {
            if (session != null) session.close(true);
            Platform.exit();
        });
        showConnectionScreen();
        stage.show();
    }

    // -------------------- API used by screens --------------------

    /** Opens the server connection and creates the session. Returns false on failure. */
    public boolean connect(Transport transport, String host, int port) {
        try {
            ServerConnection conn = ConnectionFactory.open(transport, host, port);
            // Tear down any leftover session before adopting the new connection —
            // e.g. the throwaway one used to abandon a suspended match, kept open
            // just long enough for its (async, on RMI) request to be delivered.
            if (session != null) {
                expectingDisconnect = true;
                session.close(false);
            }
            this.session = new ClientSession(conn);
            session.setHandler(this);
            this.lastTransport = transport;
            this.lastHost = host;
            this.lastPort = port;
            return true;
        } catch (Exception e) {
            showError("Unable to connect: " + ConnectionFactory.describeConnectionError(e));
            return false;
        }
    }

    public ClientSession getSession() {
        return session;
    }

    // -------------------- Screen navigation --------------------

    public void showConnectionScreen() {
        setScreen(new ConnectionScreen(this));
    }

    public void showNicknameScreen() {
        setScreen(new NicknameScreen(this));
    }

    public void showMatchesScreen(String nickname) {
        this.lastNickname = nickname;
        setScreen(new MatchesScreen(this, nickname));
    }

    /**
     * After end-of-game the server closes the channel; reopen a fresh
     * connection with the saved parameters and return to the matches screen.
     */
    public void endGameAndShowMatches() {
        if (session != null) {
            session.close(false);
            session = null;
        }
        if (lastTransport == null || lastNickname == null) {
            showConnectionScreen();
            return;
        }
        if (!connect(lastTransport, lastHost, lastPort)) {
            showConnectionScreen();
            return;
        }
        showMatchesScreen(lastNickname);
    }

    /** Aborts a running match and returns to the matches screen on the same session. */
    public void leaveMatchAndShowMatches(String nickname) {
        if (session != null) {
            try {
                session.getVirtualServer().disconnect();
            } catch (RuntimeException ignored) {
            }
            session.clearLocalMatchState();
            showMatchesScreen(nickname);
        } else {
            showConnectionScreen();
        }
    }

    /** Pre-game leave: same session reused for further list/create/join. */
    public void leaveLobbyAndShowMatches(String nickname) {
        if (session != null) {
            try {
                session.getVirtualServer().removePlayerFromLobby();
            } catch (RuntimeException ignored) {
            }
            session.clearLocalMatchState();
            showMatchesScreen(nickname);
            return;
        }
        if (lastTransport == null) {
            showConnectionScreen();
            return;
        }
        if (!connect(lastTransport, lastHost, lastPort)) {
            showConnectionScreen();
            return;
        }
        showMatchesScreen(nickname);
    }

    public void showLobbyScreen() {
        setScreen(new LobbyScreen(this));
    }

    /** Offers to resume a match the server crashed out of. */
    public void showReconnectScreen(String matchId) {
        setScreen(new ReconnectScreen(this, matchId));
    }

    /** Lobby-style waiting screen for a recovered match pending reconnections. */
    public void showRecoveryLobbyScreen() {
        setScreen(new RecoveryLobbyScreen(this));
    }

    /**
     * Reopens a connection (reusing the last transport/host/port) and asks the
     * server to resume the suspended match. Returns false if the server is
     * still unreachable; the actual outcome of the resume arrives later as a
     * GameStartedMessage (success) or an ErrorMessage (e.g. nickname mismatch).
     */
    public boolean reconnectToPreviousMatch(String matchId, String nickname) {
        if (lastTransport == null) {
            return false;
        }
        if (session != null) {
            session.close(false);
            session = null;
        }
        if (!connect(lastTransport, lastHost, lastPort)) {
            return false;
        }
        this.lastNickname = nickname;
        try {
            session.getVirtualServer().reconnect(matchId, nickname);
            return true;
        } catch (RuntimeException e) {
            showError("Reconnect failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Invoked by the reconnect screen's "Leave this match" button. Reopens a
     * connection on the transport the player first chose to tell the server to
     * delete the suspended match (and notify any player that already reconnected
     * that the match is over), then keeps that connection and drops the player on
     * the nickname scene so they can start a new match right away. Best-effort:
     * if the server is unreachable there is no connection to keep, so we fall
     * back to the initial transport-choice scene.
     */
    public void leavePreviousMatchAndShowNickname(String matchId) {
        if (lastTransport != null && connect(lastTransport, lastHost, lastPort)) {
            try {
                session.getVirtualServer().abandonRecoveredMatch(matchId);
            } catch (RuntimeException ignored) {
                // Server already gone: nothing else we can do, fall through.
            }
            // Keep the freshly opened connection. NB: we do NOT close it — on RMI
            // the request is delivered asynchronously by the outbound worker and
            // closing now would cancel it before it leaves.
            showNicknameScreen();
            return;
        }
        // Server unreachable: nothing to reuse, return to the start scene.
        showConnectionScreen();
    }

    public void showGameScreen() {
        setScreen(new GameScreen(this));
    }

    public void showEndGameScreen(EndGameMessage m) {
        String me = session != null ? session.getLocalNickname() : null;
        if (me == null) me = lastNickname;
        setScreen(new EndGameScreen(
                this,
                m.winner(),
                m.finalGameState(),
                m.leaderboard(),
                m.positionByNickname(),
                me));
    }

    private void setScreen(GuiScreen screen) {
        this.currentScreen = screen;
        Parent root = screen.getRoot();
        Scene existing = stage.getScene();
        if (existing == null) {
            Scene scene = new Scene(root, 1600, 900);
            applyStylesheet(scene);
            stage.setScene(scene);
        } else {
            existing.setRoot(root);
            applyStylesheet(existing);
        }
    }

    private void applyStylesheet(Scene scene) {
        try {
            var url = getClass().getResource("/css/game.css");
            if (url == null) return;
            String href = url.toExternalForm();
            if (!scene.getStylesheets().contains(href)) {
                scene.getStylesheets().add(href);
            }
        } catch (Exception ignored) {
            // CSS is optional.
        }
    }

    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    public void exit() {
        if (session != null) {
            session.close(true);
        }
        Platform.exit();
    }

    // -------------------- ServerHandler --------------------

    /**
     * Called from arbitrary threads (socket reader or RMI). Always re-dispatch
     * on the JavaFX thread before touching UI.
     */
    @Override
    public void onServerMessage(ServerMessage message) {
        Platform.runLater(() -> dispatchOnFxThread(message));
    }

    @Override
    public void onConnectionClosed(Throwable cause) {
        Platform.runLater(() -> {
            if (expectingDisconnect) {
                expectingDisconnect = false;
                return;
            }
            // A drop while a match is running means the server crashed: that
            // match was persisted server-side and can be resumed, so route the
            // player to the reconnect screen instead of the connection one.
            if (session != null && session.isGameStarted()
                    && session.getVirtualServer().getMatchId() != null) {
                String matchId = session.getVirtualServer().getMatchId();
                this.session = null;
                showReconnectScreen(matchId);
                return;
            }
            showError("Connection closed"
                    + (cause == null ? "." : ": " + cause.getClass().getSimpleName()));
            this.session = null;
            showConnectionScreen();
        });
    }

    private void dispatchOnFxThread(ServerMessage message) {
        // Auto-navigation driven by key messages.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(GameStartedMessage m) {
                if (currentScreen == null || !currentScreen.isGameScreen()) showGameScreen();
            }
            @Override public void visit(EndGameMessage m) {
                // The server closes the channel right after EndGameMessage;
                // silence the "Connection closed" alert/redirect.
                expectingDisconnect = true;
                showEndGameScreen(m);
            }
            @Override public void visit(MatchClosedMessage m) {
                // Aborted remotely: connection stays alive, ClientSession has
                // already cleared local state. Bounce back to the matches screen.
                showError("Match closed: " + m.reason());
                if (session != null) {
                    showMatchesScreen(lastNickname);
                } else {
                    showConnectionScreen();
                }
            }
            @Override public void visit(MatchAbandonedMessage m) {
                // A player left (or the recovery timed out): the suspended match
                // is over for everyone. The connection stays alive — the player
                // only needs to pick a nickname again to start a new match on the
                // same connection, so route them back to the nickname scene.
                showError(m.reason());
                if (session != null) {
                    showNicknameScreen();
                } else {
                    showConnectionScreen();
                }
            }
            @Override public void visit(MatchRecoveringMessage m) {
                // Reconnected, but the match is paused until everyone is back:
                // show the recovery lobby, which lists who has reconnected and
                // who is still missing. When the last player returns the server
                // sends GameStartedMessage and we move to the game.
                if (currentScreen == null || !currentScreen.isRecoveryScreen()) {
                    showRecoveryLobbyScreen();
                }
            }
            @Override public void visit(ErrorMessage m) { showError(m.message()); }
        });

        // Pre-forward navigation: a lobby confirm while on Nickname/Matches
        // pushes the Lobby screen. Must happen BEFORE forwarding because
        // those screens clear their pending-join flag on the same message.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(LobbyStateMessage m) {
                if (currentScreen != null && currentScreen.isPreLobbyScreen()) showLobbyScreen();
            }
            @Override public void visit(MatchJoinedMessage m) {
                if (currentScreen != null && currentScreen.isPreLobbyScreen()) showLobbyScreen();
            }
        });

        // Forward to the currently-mounted screen.
        GuiScreen screen = currentScreen;
        if (screen != null) {
            screen.onServerMessage(message);
        }

        // Post-forward: a GameStateMessage on an already-started session also
        // moves to the GameScreen.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(GameStateMessage m) {
                if ((currentScreen == null || !currentScreen.isGameScreen())
                        && session != null && session.isGameStarted()) showGameScreen();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
