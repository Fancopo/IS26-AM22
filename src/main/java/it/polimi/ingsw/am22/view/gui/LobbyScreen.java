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
 * Lobby screen. Shows connected players and (for the host) controls to set the
 * expected number of players. Waits for {@code GameStartedMessage} —
 * navigation is driven by {@link GuiApp}.
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

    public LobbyScreen(GuiApp app) {
        this.app = app;
        this.hostControls = new HBox(8, new Label("Expected players:"), expectedCombo, confirmExpectedButton);
        this.hostControls.setAlignment(Pos.CENTER_LEFT);
        this.root = buildUi();
        wireActions();
        // Pick up any cached lobby state replayed by the session right after join.
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
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
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
        Backgrounds.stylePanel(center);
        center.setMaxWidth(620);
        center.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane(center);
        container.setId("lobby-root");
        Backgrounds.install(container);
        return container;
    }

    private void wireActions() {
        confirmExpectedButton.setOnAction(e -> {
            Integer selected = expectedCombo.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            try {
                statusLabel.setText("Set a new expected players");
                app.getSession().getClientController().setExpectedPlayers(selected);
            } catch (RuntimeException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });
        leaveButton.setOnAction(e -> {
            String me = app.getSession().getLocalNickname();
            app.leaveLobbyAndShowMatches(me);
        });
    }

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
        hostControls.setVisible(iAmHost);
        hostControls.setManaged(iAmHost);
    }
}
