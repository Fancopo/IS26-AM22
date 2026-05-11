package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.LobbyPlayerDTO;
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Schermata di lobby.
 *
 * <p>Comportamento:
 * <ul>
 *     <li>Mostra la lista giocatori attualmente connessi.</li>
 *     <li>Se il giocatore locale è l'host, consente di impostare il numero
 *         di giocatori attesi (tipicamente 2..4 per Mesos).</li>
 *     <li>Attende il {@code GameStartedMessage}: la navigazione è gestita da {@link GuiApp}.</li>
 * </ul>
 */
public final class LobbyScreen implements GuiScreen {

    private static final Integer[] EXPECTED_PLAYERS_OPTIONS = {2, 3, 4, 5};

    private final GuiApp app;
    private final StackPane root;

    private final Label hostLabel = new Label();
    private final Label expectedLabel = new Label();
    private final Label statusLabel = new Label();
    private final ObservableList<String> playerItems = FXCollections.observableArrayList();
    private final ListView<String> playerList = new ListView<>(playerItems);
    private final ComboBox<Integer> expectedCombo = new ComboBox<>(
            FXCollections.observableArrayList(EXPECTED_PLAYERS_OPTIONS));
    private final Button confirmExpectedButton = new Button("Set expected players");
    private final Button leaveButton = new Button("Leave lobby");
    private final HBox hostControls;

    /**
     * Costruisce la schermata di lobby.
     * Invocata da {@link GuiApp#showLobbyScreen()} dopo il join/create di un
     * match. Se la {@link it.polimi.ingsw.am22.network.client.ClientSession}
     * ha gia' uno stato di lobby in cache (es. {@code LobbyStateMessage}
     * arrivato prima della creazione di questa schermata), lo renderizza subito.
     */
    public LobbyScreen(GuiApp app) {
        this.app = app;
        this.hostControls = new HBox(8, new Label("Expected players:"), expectedCombo, confirmExpectedButton);
        this.hostControls.setAlignment(Pos.CENTER_LEFT);
        this.root = buildUi();
        wireActions();
        // Render iniziale con l'eventuale stato già noto (potrebbe essere stato
        // replayato dalla ClientSession subito dopo il join).
        LobbyStateDTO cached = app.getSession().getLatestLobbyState();
        if (cached != null) render(cached);
    }

    /**
     * Restituisce il nodo radice della schermata.
     * Chiamato da {@link GuiApp#setScreen} per montare questa schermata nello stage.
     */
    @Override
    public Parent getRoot() {
        return root;
    }

    /**
     * Riceve i messaggi del server sul thread JavaFX.
     * In lobby interessa solo {@link LobbyStateMessage}: ogni volta che arriva,
     * la lista giocatori e i controlli host vengono aggiornati con {@link #render}.
     * Gli altri tipi sono gestiti da {@link GuiApp} o da altre schermate.
     */
    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(LobbyStateMessage m) { render(m.lobbyState()); }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(ErrorMessage m) {}
            @Override public void visit(InfoMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
        });
    }

    /**
     * Crea il layout JavaFX della lobby: header con host e numero di giocatori
     * attesi, lista giocatori, controlli host (visibili solo all'host), pulsante
     * "Leave lobby" e label di stato.
     */
    private StackPane buildUi() {
        expectedCombo.getSelectionModel().select(Integer.valueOf(2));

        VBox center = new VBox(12,
                new Label("Lobby"),
                hostLabel,
                expectedLabel,
                playerList,
                hostControls,
                leaveButton,
                statusLabel);
        center.setAlignment(Pos.TOP_CENTER);
        Backgrounds.stylePanel(center);
        center.setMaxWidth(620);
        center.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane(center);
        container.setId("lobby-root");
        Backgrounds.install(container);
        return container;
    }

    /**
     * Collega i listener ai pulsanti della schermata:
     * <ul>
     *   <li>{@code confirmExpectedButton}: solo per l'host — invia al server
     *       il numero di giocatori attesi via {@code setExpectedPlayers};</li>
     *   <li>{@code leaveButton}: invoca {@link GuiApp#leaveLobbyAndShowMatches}
     *       mantenendo la sessione e tornando alla MatchesScreen.</li>
     * </ul>
     */
    private void wireActions() {
        confirmExpectedButton.setOnAction(e -> {
            Integer selected = expectedCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                statusLabel.setText("Setting expected players...");
                app.getSession().getClientController().setExpectedPlayers(selected);
            } catch (RuntimeException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });
        leaveButton.setOnAction(e -> {
            // Conserviamo il nickname locale prima del leave: il server chiuderà
            // il canale e GuiApp aprirà una nuova connessione tornando alla
            // schermata di selezione partita con lo stesso nickname.
            String me = app.getSession().getLocalNickname();
            app.leaveLobbyAndShowMatches(me);
        });
    }

    /**
     * Aggiorna i nodi UI in base al nuovo {@link LobbyStateDTO}.
     * Mostra/nasconde i controlli host in base al nickname locale.
     */
    private void render(LobbyStateDTO lobby) {
        hostLabel.setText("Host: " + (lobby.hostNickname() == null ? "(unknown)" : lobby.hostNickname()));
        expectedLabel.setText("Expected players: "
                + (lobby.expectedPlayers() > 0 ? lobby.expectedPlayers() : "(not set)"));
        playerItems.clear();
        for (LobbyPlayerDTO p : lobby.players()) {
            playerItems.add(p.nickname()
                    + (p.host() ? " (host)" : "")
                    + (p.totemColor() == null ? "" : " [" + p.totemColor() + "]"));
        }
        String me = app.getSession().getLocalNickname();
        boolean iAmHost = me != null && me.equalsIgnoreCase(lobby.hostNickname());
        // Solo l'host vede i controlli per impostare il numero di giocatori attesi.
        hostControls.setVisible(iAmHost);
        hostControls.setManaged(iAmHost);
    }
}
