package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Schermata di fine partita.
 *
 * <p>Mostra il vincitore e il riepilogo finale di tutti i giocatori.
 * Il pulsante "Close" chiude la sessione e termina l'applicazione.
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
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        box.getChildren().add(new Label("*** GAME OVER ***"));
        if (winner != null) {
            box.getChildren().add(new Label("Winner: " + winner.nickname()
                    + "  [" + winner.totemColor() + "]"
                    + "  PP=" + winner.finalPrestigePoints()
                    + "  food=" + winner.remainingFood()));
        }
        box.getChildren().add(new Label("Final standings:"));
        if (finalState != null) {
            VBox table = new VBox(4);
            for (PlayerDTO p : finalState.players()) {
                table.getChildren().add(new Label(String.format("%s [%s]  PP=%d  food=%d",
                        p.nickname(),
                        p.totemColor(),
                        p.prestigePoints(),
                        p.food())));
            }
            box.getChildren().add(table);
        }

        Button close = new Button("Close");
        close.setOnAction(e -> app.exit());
        box.getChildren().add(close);

        // GRAPHIC PLACEHOLDER: sfondo schermata vittoria.
        StackPane container = new StackPane(box);
        container.setId("endgame-root");
        return container;
    }
}
