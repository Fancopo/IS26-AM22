package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ServerMessageDispatcher;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * JavaFX root: owns the {@link Stage} and the {@link ClientSession}, acts as
 * the {@link ServerMessageDispatcher} (marshalling messages onto the FX thread),
 * and drives screen navigation: Connection → Nickname → Matches → Lobby →
 * Game → EndGame. Each screen's {@code onServerMessage(...)} is always
 * called on the FX thread.
 */
public final class GuiApp extends Application implements ServerMessageDispatcher {

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
            this.session = new ClientSession(conn);
            session.setHandler(this);
            this.lastTransport = transport;
            this.lastHost = host;
            this.lastPort = port;
            return true;
        } catch (Exception e) {
            showError("Unable to connect: " + e.getMessage());
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
                session.getClientController().disconnect();
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
                session.getClientController().removePlayerFromLobby();
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

    // -------------------- ServerMessageDispatcher --------------------

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
                if (!(currentScreen instanceof GameScreen)) showGameScreen();
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
            @Override public void visit(ErrorMessage m) { showError(m.message()); }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(MatchJoinedMessage m) {}
            @Override public void visit(MatchesListMessage m) {}
        });

        // Pre-forward navigation: a lobby confirm while on Nickname/Matches
        // pushes the Lobby screen. Must happen BEFORE forwarding because
        // those screens clear their pending-join flag on the same message.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(LobbyStateMessage m) {
                if (currentScreen instanceof NicknameScreen
                        || currentScreen instanceof MatchesScreen) showLobbyScreen();
            }
            @Override public void visit(MatchJoinedMessage m) {
                if (currentScreen instanceof NicknameScreen
                        || currentScreen instanceof MatchesScreen) showLobbyScreen();
            }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(MatchesListMessage m) {}
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
                if (!(currentScreen instanceof GameScreen) && session != null && session.isGameStarted()) showGameScreen();
            }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(MatchJoinedMessage m) {}
            @Override public void visit(MatchesListMessage m) {}
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}
