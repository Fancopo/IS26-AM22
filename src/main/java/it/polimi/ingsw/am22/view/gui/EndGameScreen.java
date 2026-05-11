package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
 * <p>Mostra il vincitore, la classifica della partita appena finita,
 * la posizione del giocatore locale nella classifica storica delle
 * partite con lo stesso numero di giocatori, e un pulsante per
 * visualizzare la classifica storica completa.
 */
public final class EndGameScreen implements GuiScreen {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final GuiApp app;
    private final StackPane root;
    private final List<LeaderboardEntryDTO> historicalLeaderboard;
    private final int numPlayers;

    /**
     * Costruisce la schermata di fine partita a partire dai dati ricevuti
     * nell'{@link EndGameMessage}.
     * Invocata da {@link GuiApp#showEndGameScreen}.
     *
     * @param app                    riferimento all'applicazione (per navigazione/exit)
     * @param winner                 vincitore dichiarato dal server (puo' essere null)
     * @param finalState             stato finale di gioco usato per la classifica
     * @param historicalLeaderboard  classifica storica delle partite con lo stesso numero di giocatori
     * @param positionByNickname     posizione del giocatore locale nella classifica storica
     * @param localNickname          nickname del giocatore locale (per evidenziarlo)
     */
    public EndGameScreen(GuiApp app,
                         WinnerDTO winner,
                         GameStateDTO finalState,
                         List<LeaderboardEntryDTO> historicalLeaderboard,
                         Map<String, Integer> positionByNickname,
                         String localNickname) {
        this.app = app;
        this.historicalLeaderboard = historicalLeaderboard == null
                ? List.of() : historicalLeaderboard;
        this.numPlayers = finalState == null ? 0 : finalState.players().size();
        this.root = buildUi(winner, finalState,
                positionByNickname == null ? Map.of() : positionByNickname,
                localNickname);
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
     * Costruisce l'intero layout della schermata di fine partita:
     * titolo "GAME OVER", box dorato con il vincitore, tabella della
     * classifica della partita appena finita, label con la posizione
     * del giocatore nella classifica storica e una barra di pulsanti
     * (mostra classifica, torna alle partite, esci).
     */
    private StackPane buildUi(WinnerDTO winner,
                              GameStateDTO finalState,
                              Map<String, Integer> positionByNickname,
                              String localNickname) {
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

        Label standingsTitle = new Label("Final standings (this match)");
        standingsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<PlayerDTO> table = buildStandingsTable(finalState, winner);

        Label positionLabel = buildPositionLabel(positionByNickname, localNickname);

        Button leaderboardButton = new Button("Show full leaderboard");
        leaderboardButton.setOnAction(e -> showLeaderboardDialog());
        leaderboardButton.setDisable(historicalLeaderboard.isEmpty());

        Button backButton = new Button("Back to matches");
        backButton.setOnAction(e -> app.endGameAndShowMatches());
        Button exitButton = new Button("Exit");
        exitButton.setOnAction(e -> app.exit());

        HBox buttons = new HBox(12, leaderboardButton, backButton, exitButton);
        buttons.setAlignment(Pos.CENTER);

        VBox panel = new VBox(14,
                title,
                winnerBox,
                standingsTitle,
                table,
                positionLabel,
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

    /**
     * Crea la label che mostra la posizione del giocatore locale nella
     * classifica storica per partite con lo stesso numero di giocatori.
     * Se la classifica non e' disponibile o il nickname non c'e', mostra
     * un messaggio alternativo.
     */
    private Label buildPositionLabel(Map<String, Integer> positionByNickname,
                                     String localNickname) {
        Label label = new Label();
        label.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        if (positionByNickname.isEmpty()) {
            label.setText("Historical leaderboard unavailable.");
            label.setStyle(label.getStyle() + " -fx-text-fill: #a04040;");
            return label;
        }

        Integer pos = localNickname == null ? null : positionByNickname.get(localNickname);
        if (pos == null) {
            label.setText("Your position is not available for this match.");
            return label;
        }

        label.setText(String.format(
                "Your position in all %d-player matches: #%d (out of %d)",
                numPlayers, pos, historicalLeaderboard.size()));
        return label;
    }

    /**
     * Apre una finestra modale con la classifica storica completa
     * (#, giocatore, score, data) delle partite con lo stesso numero
     * di giocatori. Invocata dal pulsante "Show full leaderboard".
     */
    private void showLeaderboardDialog() {
        TableView<LeaderboardEntryDTO> table = new TableView<>(
                FXCollections.observableArrayList(historicalLeaderboard));
        table.setPlaceholder(new Label("(no data)"));

        TableColumn<LeaderboardEntryDTO, String> rankCol = new TableColumn<>("#");
        rankCol.setCellValueFactory(c -> {
            int idx = historicalLeaderboard.indexOf(c.getValue()) + 1;
            return new SimpleStringProperty(String.valueOf(idx));
        });
        rankCol.setPrefWidth(40);

        TableColumn<LeaderboardEntryDTO, String> nameCol = new TableColumn<>("Player");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().nickname()));
        nameCol.setPrefWidth(200);

        TableColumn<LeaderboardEntryDTO, Number> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().score()));
        scoreCol.setPrefWidth(90);

        TableColumn<LeaderboardEntryDTO, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().endDate() == null ? "" : c.getValue().endDate().format(DATE_FMT)));
        dateCol.setPrefWidth(160);

        table.getColumns().add(rankCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(scoreCol);
        table.getColumns().add(dateCol);
        table.setPrefHeight(360);
        table.setPrefWidth(540);

        Label header = new Label("Leaderboard — " + numPlayers + "-player matches");
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button close = new Button("Close");
        VBox content = new VBox(10, header, table, close);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(16));

        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.setTitle("Historical leaderboard");
        dialog.initOwner(root.getScene() == null ? null : root.getScene().getWindow());
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setScene(new javafx.scene.Scene(content));
        close.setOnAction(e -> dialog.close());
        dialog.showAndWait();
    }

    /**
     * Costruisce la tabella della classifica della partita appena finita.
     * I giocatori sono ordinati per punti prestigio decrescenti (tie-break
     * con cibo decrescente). Il vincitore e' marcato con un asterisco.
     */
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
