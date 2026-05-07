package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Schermata di fine partita.
 *
 * <p>Mostra il vincitore, la classifica finale e due azioni:
 * tornare alla {@link MatchesScreen} o uscire dall'applicazione.
 */
public final class EndGameScreen implements GuiScreen {

    private final GuiApp app;
    private final StackPane root;

    public EndGameScreen(GuiApp app, WinnerDTO winner, GameStateDTO finalState) {
        this.app = app;
        this.root = buildUi(winner, finalState);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    private StackPane buildUi(WinnerDTO winner, GameStateDTO finalState) {
        Label title = new Label("GAME OVER");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold;");

        VBox winnerBox = new VBox(6);
        winnerBox.setAlignment(Pos.CENTER);
        winnerBox.setPadding(new Insets(16));
        winnerBox.setStyle("-fx-background-color: rgba(255,215,0,0.30);"
                + " -fx-background-radius: 10;"
                + " -fx-border-color: rgba(160,120,0,0.6);"
                + " -fx-border-width: 2;"
                + " -fx-border-radius: 10;");

        if (winner != null) {
            Label crown = new Label("WINNER");
            crown.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"
                    + " -fx-text-fill: #8a6b00;");
            Label name = new Label(winner.nickname());
            name.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
            Label stats = new Label(String.format(
                    "Totem: %s    Prestige Points: %d    Food: %d",
                    winner.totemColor(),
                    winner.finalPrestigePoints(),
                    winner.remainingFood()));
            stats.setStyle("-fx-font-size: 14px;");
            winnerBox.getChildren().addAll(crown, name, stats);
        } else {
            Label noWinner = new Label("No winner declared");
            noWinner.setStyle("-fx-font-size: 18px;");
            winnerBox.getChildren().add(noWinner);
        }

        Label standingsTitle = new Label("Final standings");
        standingsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<PlayerDTO> table = buildStandingsTable(finalState, winner);

        Button backButton = new Button("Back to matches");
        backButton.setOnAction(e -> app.endGameAndShowMatches());
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> app.exit());

        HBox buttons = new HBox(12, backButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox panel = new VBox(16,
                title,
                winnerBox,
                standingsTitle,
                table,
                buttons);
        panel.setAlignment(Pos.TOP_CENTER);
        Backgrounds.stylePanel(panel);
        panel.setMaxWidth(720);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane container = new StackPane(panel);
        container.setId("endgame-root");
        Backgrounds.install(container, "/images/background_noMESOS.png");
        return container;
    }

    private TableView<PlayerDTO> buildStandingsTable(GameStateDTO finalState, WinnerDTO winner) {
        List<PlayerDTO> players = new ArrayList<>(
                finalState == null ? List.of() : finalState.players());
        players.sort(Comparator
                .comparingInt(PlayerDTO::prestigePoints).reversed()
                .thenComparingInt(PlayerDTO::food).reversed());

        TableView<PlayerDTO> table = new TableView<>(FXCollections.observableArrayList(players));
        table.setPlaceholder(new Label("(no player data)"));

        TableColumn<PlayerDTO, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(c -> {
            int idx = players.indexOf(c.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        rankCol.setPrefWidth(40);

        TableColumn<PlayerDTO, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(c -> {
            String n = c.getValue().nickname();
            boolean isWinner = winner != null && n.equals(winner.nickname());
            return new SimpleStringProperty(isWinner ? n + "  *" : n);
        });
        nameCol.setPrefWidth(200);

        TableColumn<PlayerDTO, String> totemCol = new TableColumn<>("Totem");
        totemCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().totemColor()));
        totemCol.setPrefWidth(100);

        TableColumn<PlayerDTO, Number> ppCol = new TableColumn<>("Prestige");
        ppCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().prestigePoints()));
        ppCol.setPrefWidth(90);

        TableColumn<PlayerDTO, Number> foodCol = new TableColumn<>("Food");
        foodCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().food()));
        foodCol.setPrefWidth(70);

        table.getColumns().add(rankCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(totemCol);
        table.getColumns().add(ppCol);
        table.getColumns().add(foodCol);
        table.setPrefHeight(220);
        return table;
    }
}
