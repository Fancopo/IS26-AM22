package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
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

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Match selection screen: list open matches, join one, or create a new one.
 * Navigation to {@link LobbyScreen} is driven by {@link GuiApp} on the first
 * {@link LobbyStateMessage}/{@link MatchJoinedMessage}.
 */
public final class MatchesScreen implements GuiScreen {

    private static final Integer[] EXPECTED_PLAYERS_OPTIONS = {2, 3, 4, 5};

    private final GuiApp app;
    private final String nickname;
    private final StackPane root;

    private final ObservableList<MatchInfoDTO> matches = FXCollections.observableArrayList();
    private final TableView<MatchInfoDTO> matchesTable = new TableView<>(matches);
    private final ComboBox<Integer> expectedCombo = new ComboBox<>(
            FXCollections.observableArrayList(EXPECTED_PLAYERS_OPTIONS));
    private final Button refreshButton = new Button("Refresh");
    private final Button joinButton = new Button("Join selected");
    private final Button createButton = new Button("Create new match");
    private final Button backButton = new Button("Back");
    private final Label nicknameLabel = new Label();
    private final Label statusLabel = new Label();

    private boolean pendingAction;

    public MatchesScreen(GuiApp app, String nickname) {
        this.app = app;
        this.nickname = nickname;
        this.root = buildUi();
        wireActions();
        requestList();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(MatchesListMessage m) {
                matches.setAll(m.matches());
                statusLabel.setText(m.matches().isEmpty()
                        ? "No open matches — create one!"
                        : m.matches().size() + " open match(es)");
            }
            @Override public void visit(ErrorMessage m) {
                if (pendingAction) {
                    pendingAction = false;
                    setControlsDisabled(false);
                }
                statusLabel.setText("Error: " + m.message());
            }
            @Override public void visit(LobbyStateMessage m) { pendingAction = false; }
            @Override public void visit(MatchJoinedMessage m) { pendingAction = false; }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(InfoMessage m) {}
        });
    }

    private StackPane buildUi() {
        nicknameLabel.setText("Playing as: " + nickname);

        TableColumn<MatchInfoDTO, String> idCol = new TableColumn<>("Match ID");
        idCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().matchId()));
        idCol.setPrefWidth(220);

        TableColumn<MatchInfoDTO, String> hostCol = new TableColumn<>("Host");
        hostCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().hostNickname()));
        hostCol.setPrefWidth(160);

        TableColumn<MatchInfoDTO, Number> currentCol = new TableColumn<>("Players");
        currentCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().currentPlayers()));
        currentCol.setPrefWidth(80);

        TableColumn<MatchInfoDTO, Number> expectedCol = new TableColumn<>("Expected");
        expectedCol.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(c.getValue().expectedPlayers()));
        expectedCol.setPrefWidth(80);

        TableColumn<MatchInfoDTO, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().started() ? "started" : "open"));
        statusCol.setPrefWidth(90);

        matchesTable.getColumns().add(idCol);
        matchesTable.getColumns().add(hostCol);
        matchesTable.getColumns().add(currentCol);
        matchesTable.getColumns().add(expectedCol);
        matchesTable.getColumns().add(statusCol);
        matchesTable.setPrefHeight(320);
        matchesTable.setPlaceholder(new Label("(no open matches yet — refresh or create one)"));

        expectedCombo.getSelectionModel().select(Integer.valueOf(2));

        HBox topBar = new HBox(10, nicknameLabel, refreshButton);
        topBar.setAlignment(Pos.CENTER_LEFT);

        HBox joinBar = new HBox(10, joinButton);
        joinBar.setAlignment(Pos.CENTER_LEFT);

        HBox createBar = new HBox(10,
                new Label("Expected players:"),
                expectedCombo,
                createButton);
        createBar.setAlignment(Pos.CENTER_LEFT);

        VBox center = new VBox(14,
                new Label("Available matches"),
                topBar,
                matchesTable,
                joinBar,
                new Label("— or —"),
                createBar,
                backButton,
                statusLabel);
        center.setAlignment(Pos.TOP_CENTER);
        Backgrounds.stylePanel(center);
        center.setMaxWidth(720);
        center.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane(center);
        container.setId("matches-root");
        Backgrounds.install(container);
        return container;
    }

    private void wireActions() {
        refreshButton.setOnAction(e -> requestList());

        joinButton.setOnAction(e -> {
            MatchInfoDTO selected = matchesTable.getSelectionModel().getSelectedItem();
            if (selected == null) {
                statusLabel.setText("Please select a match to join.");
                return;
            }
            if (selected.started()) {
                statusLabel.setText("That match has already started.");
                return;
            }
            try {
                statusLabel.setText("Joining " + selected.matchId() + "...");
                pendingAction = true;
                setControlsDisabled(true);
                app.getSession().getClientController()
                        .addPlayerToLobby(selected.matchId(), nickname);
            } catch (RuntimeException ex) {
                pendingAction = false;
                setControlsDisabled(false);
                statusLabel.setText("Join failed: " + ex.getMessage());
            }
        });

        createButton.setOnAction(e -> {
            Integer expected = expectedCombo.getSelectionModel().getSelectedItem();
            if (expected == null) {
                statusLabel.setText("Please pick the expected number of players.");
                return;
            }
            try {
                statusLabel.setText("Creating new match (" + expected + " players)...");
                pendingAction = true;
                setControlsDisabled(true);
                app.getSession().getClientController().createMatch(nickname, expected);
            } catch (RuntimeException ex) {
                pendingAction = false;
                setControlsDisabled(false);
                statusLabel.setText("Create failed: " + ex.getMessage());
            }
        });

        backButton.setOnAction(e -> app.showNicknameScreen());
    }

    private void requestList() {
        try {
            statusLabel.setText("Refreshing...");
            app.getSession().getClientController().listMatches();
        } catch (RuntimeException ex) {
            statusLabel.setText("Refresh failed: " + ex.getMessage());
        }
    }

    /** Locked while waiting for a join/create reply, to prevent double-clicks. */
    private void setControlsDisabled(boolean disabled) {
        joinButton.setDisable(disabled);
        createButton.setDisable(disabled);
        refreshButton.setDisable(disabled);
    }
}
