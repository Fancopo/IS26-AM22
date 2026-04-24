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
import javafx.geometry.Insets;
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

    private static final Integer[] EXPECTED_PLAYERS_OPTIONS = {2, 3, 4};

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

    @Override
    public Parent getRoot() {
        return root;
    }

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
        });
    }

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
        center.setPadding(new Insets(30));

        // GRAPHIC PLACEHOLDER: questo StackPane è il contenitore su cui
        // si potrà sovrapporre lo sfondo della lobby.
        StackPane container = new StackPane(center);
        container.setId("lobby-root");
        return container;
    }

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
            try {
                app.getSession().getClientController().removePlayerFromLobby();
            } catch (RuntimeException ignored) {
                // la connection potrebbe essere già giù; procediamo comunque
            }
            app.getSession().close(false);
            app.showConnectionScreen();
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
