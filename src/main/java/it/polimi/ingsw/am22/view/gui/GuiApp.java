package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;
import it.polimi.ingsw.am22.network.client.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.ObservableServerConnection;
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

    /** Parametri dell'ultima connessione riuscita, usati per riconnettersi
     *  dopo un leave volontario senza tornare alla schermata di connessione. */
    private Transport lastTransport;
    private String lastHost;
    private int lastPort;
    /** Ultimo nickname usato: serve per tornare alla MatchesScreen dopo la fine partita. */
    private String lastNickname;

    /** Quando true, la prossima onConnectionClosed non mostra alert né torna
     *  alla ConnectionScreen: la chiusura era voluta (es. leave dalla lobby). */
    private boolean expectingDisconnect;

    /**
     * Punto di ingresso dell'applicazione JavaFX.
     * Invocato automaticamente da {@link Application#launch} (vedi {@link #main}
     * oppure {@code ClientApp}). Inizializza lo stage, registra l'handler di
     * chiusura della finestra e mostra la {@link ConnectionScreen}.
     */
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
            this.lastTransport = transport;
            this.lastHost = host;
            this.lastPort = port;
            return true;
        } catch (Exception e) {
            showError("Unable to connect: " + e.getMessage());
            return false;
        }
    }

    /**
     * Accesso alla {@link ClientSession} corrente.
     * Usato dalle schermate per inoltrare comandi al server tramite
     * {@code session.getClientController()} e per leggere lo stato locale
     * (nickname, ultimo gameState/lobbyState, ecc.). Restituisce {@code null}
     * se non e' stata ancora aperta una connessione.
     */
    public ClientSession getSession() {
        return session;
    }

    // -------------------- Navigazione schermate --------------------

    /**
     * Mostra la {@link ConnectionScreen}: prima schermata della GUI, dove
     * l'utente sceglie trasporto/host/porta.
     */
    public void showConnectionScreen() {
        setScreen(new ConnectionScreen(this));
    }

    /**
     * Mostra la {@link NicknameScreen}: l'utente sceglie il proprio nickname
     * prima di entrare nella schermata di selezione partita.
     */
    public void showNicknameScreen() {
        setScreen(new NicknameScreen(this));
    }

    /**
     * Mostra la {@link MatchesScreen} per il nickname indicato e memorizza
     * il nickname come {@link #lastNickname} per poterlo riutilizzare
     * dopo una fine partita o un leave volontario.
     */
    public void showMatchesScreen(String nickname) {
        this.lastNickname = nickname;
        setScreen(new MatchesScreen(this, nickname));
    }

    /**
     * Torna alla {@link MatchesScreen} dopo la fine di una partita.
     * Il server chiude il canale a fine partita: riapriamo una connessione
     * pulita riusando gli ultimi parametri (transport/host/port/nickname).
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

    /**
     * Esce da una partita già iniziata e torna alla {@link MatchesScreen}.
     * Invia al server una {@code disconnectPlayer}: lato server la partita
     * viene abortita per tutti i partecipanti, ma nessun canale viene chiuso.
     * La sessione del giocatore resta viva e può subito list/create/join un
     * nuovo match.
     */
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

    /**
     * Esce dalla lobby corrente (pre-game) e torna alla {@link MatchesScreen}
     * mantenendo la stessa sessione: il server non chiude il canale, quindi
     * il giocatore può subito vedere/creare/joinare un'altra partita.
     */
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

    /**
     * Mostra la {@link LobbyScreen}: invocato automaticamente quando arriva
     * il primo {@link LobbyStateMessage} (o {@link it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage})
     * dal server dopo che il giocatore ha creato o unito un match.
     */
    public void showLobbyScreen() {
        setScreen(new LobbyScreen(this));
    }

    /**
     * Mostra la {@link GameScreen}: invocato quando arriva
     * {@link GameStartedMessage} oppure quando lo stato della sessione
     * indica che la partita e' iniziata.
     */
    public void showGameScreen() {
        setScreen(new GameScreen(this));
    }

    /**
     * Mostra la {@link EndGameScreen} costruita a partire dall'{@link EndGameMessage}
     * ricevuto: passa vincitore, stato finale, classifica storica e la posizione
     * del giocatore locale in quella classifica.
     */
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
            Scene scene = new Scene(root, 1600, 900);
            applyStylesheet(scene);
            stage.setScene(scene);
        } else {
            existing.setRoot(root);
            applyStylesheet(existing);
        }
    }

    /** Carica il CSS della GameScreen una sola volta (idempotente). */
    private void applyStylesheet(Scene scene) {
        try {
            var url = getClass().getResource("/css/game.css");
            if (url == null) return;
            String href = url.toExternalForm();
            if (!scene.getStylesheets().contains(href)) {
                scene.getStylesheets().add(href);
            }
        } catch (Exception ignored) {
            // CSS opzionale: la UI funziona anche senza.
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

    /**
     * Callback invocata dalla {@link ClientSession} per ogni messaggio in
     * arrivo dal server. Puo' essere chiamata da thread arbitrari (reader
     * thread del socket o thread RMI), per questo il messaggio viene sempre
     * rimbalzato sul JavaFX thread tramite {@link Platform#runLater}.
     */
    @Override
    public void onServerMessage(ServerMessage message) {
        // I messaggi arrivano sul reader thread socket o sul thread RMI:
        // li rimbalziamo SEMPRE sul JavaFX thread prima di toccare la UI.
        Platform.runLater(() -> dispatchOnFxThread(message));
    }

    /**
     * Callback invocata dalla {@link ClientSession} quando la connessione
     * verso il server viene chiusa. Se la disconnessione era attesa
     * ({@link #expectingDisconnect} = true, es. fine partita o leave volontario)
     * la callback e' silenziosa, altrimenti viene mostrato un alert e si torna
     * alla {@link ConnectionScreen}.
     */
    @Override
    public void onConnectionClosed(Throwable cause) {
        Platform.runLater(() -> {
            if (expectingDisconnect) {
                // Disconnessione attesa (es. leave volontario): silenziosa.
                expectingDisconnect = false;
                return;
            }
            showError("Connection closed"
                    + (cause == null ? "." : ": " + cause.getClass().getSimpleName()));
            // Dopo una disconnessione inattesa riportiamo il client alla schermata di connessione.
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
                // Il server chiude il canale subito dopo l'EndGameMessage:
                // sopprimiamo l'alert di "Connection closed" e il redirect
                // alla ConnectionScreen che ne deriverebbe.
                expectingDisconnect = true;
                showEndGameScreen(m);
            }
            @Override public void visit(MatchClosedMessage m) {
                // Match abortito da remoto: la connessione col server resta
                // viva (il server non chiude più i canali) e ClientSession ha
                // già azzerato gli snapshot e il binding del controller.
                // Riportiamo l'utente alla MatchesScreen mantenendo la stessa
                // sessione, così può subito list/create/join un altro match.
                showError("Match closed: " + m.reason());
                if (session != null) {
                    showMatchesScreen(lastNickname);
                } else {
                    showConnectionScreen();
                }
            }
            @Override public void visit(ErrorMessage m) { showError(m.message()); }
            @Override public void visit(InfoMessage m) { System.out.println("[INFO] " + m.message()); }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
        });

        // Navigazione PRE-inoltro: se la NicknameScreen è ancora attiva e arriva
        // una conferma di lobby (LobbyStateMessage o MatchJoinedMessage),
        // passiamo subito alla LobbyScreen. Va fatto PRIMA dell'inoltro perché
        // NicknameScreen.onServerMessage azzera il flag pendingJoin.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(LobbyStateMessage m) {
                if (currentScreen instanceof NicknameScreen
                        || currentScreen instanceof MatchesScreen) showLobbyScreen();
            }
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {
                if (currentScreen instanceof NicknameScreen
                        || currentScreen instanceof MatchesScreen) showLobbyScreen();
            }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(InfoMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
        });

        // Inoltriamo alla schermata attiva (eventualmente quella appena installata).
        GuiScreen screen = currentScreen;
        if (screen != null) {
            screen.onServerMessage(message);
        }

        // Navigazione post-inoltro: per il GameStateMessage, se la sessione
        // segnala che la partita è iniziata, passiamo alla GameScreen.
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(GameStateMessage m) {
                if (!(currentScreen instanceof GameScreen) && session != null && session.isGameStarted()) showGameScreen();
            }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(InfoMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
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
