package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;
import it.polimi.ingsw.am22.network.client.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.ObservableServerConnection;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

/**
 * Applicazione JavaFX principale del client.
 *
 * <p>Ruolo nel pattern MVC: orchestratore della View grafica.
 * <ul>
 *     <li>Possiede lo {@link Stage} e la {@link ClientSession}.</li>
 *     <li>Fa da {@link ClientUpdateHandler}: riceve i messaggi dal server
 *         (su thread arbitrari) e li ridispiazza sul thread JavaFX.</li>
 *     <li>Gestisce la navigazione tra schermate:
 *         Connection &rarr; Nickname &rarr; Lobby &rarr; Game &rarr; EndGame.</li>
 * </ul>
 *
 * <p>Le singole schermate espongono un metodo {@code onServerMessage(...)}
 * che viene invocato sempre sul JavaFX thread: possono quindi aggiornare
 * i nodi UI senza ulteriori accorgimenti di threading.
 */
public final class GuiApp extends Application implements ClientUpdateHandler {

    private Stage stage;
    private ClientSession session;

    /** Schermata attualmente visibile, destinataria dei {@code ServerMessage}. */
    private GuiScreen currentScreen;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("MESOS");
        // Se l'utente chiude la finestra con la X, chiudiamo la sessione in modo pulito.
        stage.setOnCloseRequest(e -> {
            if (session != null) session.close(true);
            Platform.exit();
        });
        showConnectionScreen();
        stage.show();
    }

    // -------------------- API usate dalle schermate --------------------

    /**
     * Apre la connessione verso il server e crea la {@link ClientSession}.
     * Va chiamata dalla ConnectionScreen.
     *
     * @return {@code true} in caso di successo, {@code false} altrimenti
     */
    public boolean connect(Transport transport, String host, int port) {
        try {
            ObservableServerConnection conn = ConnectionFactory.open(transport, host, port);
            this.session = new ClientSession(conn);
            // Registriamo GuiApp come handler: filtra e inoltra alla schermata attiva.
            session.setHandler(this);
            return true;
        } catch (Exception e) {
            showError("Unable to connect: " + e.getMessage());
            return false;
        }
    }

    public ClientSession getSession() {
        return session;
    }

    // -------------------- Navigazione schermate --------------------

    public void showConnectionScreen() {
        setScreen(new ConnectionScreen(this));
    }

    public void showNicknameScreen() {
        setScreen(new NicknameScreen(this));
    }

    public void showLobbyScreen() {
        setScreen(new LobbyScreen(this));
    }

    public void showGameScreen() {
        setScreen(new GameScreen(this));
    }

    public void showEndGameScreen(WinnerDTO winner, GameStateDTO finalState) {
        setScreen(new EndGameScreen(this, winner, finalState));
    }

    /**
     * Installa una nuova schermata nello stage.
     * La Scene viene creata o aggiornata con una dimensione di default sufficiente
     * per tutti i layout: l'utente potrà poi sostituire i nodi con le risorse grafiche.
     */
    private void setScreen(GuiScreen screen) {
        this.currentScreen = screen;
        Parent root = screen.getRoot();
        Scene existing = stage.getScene();
        if (existing == null) {
            stage.setScene(new Scene(root, 1100, 720));
        } else {
            existing.setRoot(root);
        }
    }

    /** Mostra un alert non bloccante con un messaggio di errore. */
    public void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR, message);
            alert.setHeaderText(null);
            alert.showAndWait();
        });
    }

    /** Termina l'applicazione chiudendo la sessione se presente. */
    public void exit() {
        if (session != null) {
            session.close(true);
        }
        Platform.exit();
    }

    // -------------------- ClientUpdateHandler (dal server) --------------------

    @Override
    public void onServerMessage(ServerMessage message) {
        // I messaggi arrivano sul reader thread socket o sul thread RMI:
        // li rimbalziamo SEMPRE sul JavaFX thread prima di toccare la UI.
        Platform.runLater(() -> dispatchOnFxThread(message));
    }

    @Override
    public void onConnectionClosed(Throwable cause) {
        Platform.runLater(() -> {
            showError("Connection closed"
                    + (cause == null ? "." : ": " + cause.getClass().getSimpleName()));
            // Dopo una disconnessione riportiamo il client alla schermata di connessione.
            this.session = null;
            showConnectionScreen();
        });
    }

    /** Dispatch eseguito sempre sul JavaFX thread. */
    private void dispatchOnFxThread(ServerMessage message) {
        // Navigazione automatica guidata dai messaggi chiave.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(GameStartedMessage m) {
                if (!(currentScreen instanceof GameScreen)) showGameScreen();
            }
            @Override public void visit(EndGameMessage m) {
                showEndGameScreen(m.winner(), m.finalGameState());
            }
            @Override public void visit(MatchClosedMessage m) {
                showError("Match closed: " + m.reason());
                if (session != null) { session.close(false); session = null; }
                showConnectionScreen();
            }
            @Override public void visit(ErrorMessage m) { showError(m.message()); }
            @Override public void visit(InfoMessage m) { System.out.println("[INFO] " + m.message()); }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStateMessage m) {}
        });

        // Inoltriamo alla schermata attiva (ConnectionScreen ignora MatchClosedMessage via default).
        GuiScreen screen = currentScreen;
        if (screen != null) {
            screen.onServerMessage(message);
        }

        // Navigazione post-inoltro basata sul tipo di messaggio e sullo stato corrente.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(LobbyStateMessage m) {
                if (currentScreen instanceof NicknameScreen ns && ns.hasPendingJoin()) showLobbyScreen();
            }
            @Override public void visit(GameStateMessage m) {
                if (!(currentScreen instanceof GameScreen) && session != null && session.isGameStarted()) showGameScreen();
            }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(InfoMessage m) {}
        });
    }

    /**
     * Main opzionale: consente di lanciare direttamente la GUI senza passare
     * da {@code ClientApp}. Normalmente il punto d'ingresso è {@code ClientApp}.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
