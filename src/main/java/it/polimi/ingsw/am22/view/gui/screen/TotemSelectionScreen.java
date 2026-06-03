package it.polimi.ingsw.am22.view.gui.screen;

import it.polimi.ingsw.am22.network.protocol.dto.TotemOptionDTO;
import it.polimi.ingsw.am22.network.protocol.dto.TotemSelectionStateDTO;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.message.response.TotemSelectionMessage;
import it.polimi.ingsw.am22.view.gui.GuiApp;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * Pre-game totem selection scene. Players pick a totem color in turn (lobby
 * join order). The player whose turn it is can click a free totem — it lights
 * up — and press Confirm to lock it; everyone else sees a "waiting" message and
 * cannot click. A totem already taken shows its owner's nickname and is
 * disabled for the others. When the last player has chosen, the server sends a
 * {@code GameStartedMessage} and {@link GuiApp} moves to the game scene.
 */
public final class TotemSelectionScreen implements GuiScreen {

    private static final double TOTEM_SIZE = 150;

    private final GuiApp app;
    private final StackPane root;

    private final Label titleLabel = new Label("Choose your totem");
    private final Label statusLabel = new Label();
    private final FlowPane totemPane = new FlowPane(18, 18);
    private final Button confirmButton = new Button("Confirm");

    /** Color currently clicked by this player, pending confirmation. */
    private String pendingColor;
    /** True between pressing Confirm and the server's next update. */
    private boolean submitting;

    public TotemSelectionScreen(GuiApp app) {
        this.app = app;
        this.root = buildUi();
        wireActions();
        TotemSelectionStateDTO cached = app.getSession().getLatestTotemSelectionState();
        if (cached != null) render(cached);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public boolean isTotemSelectionScreen() {
        return true;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(TotemSelectionMessage m) { render(m.selectionState()); }
        });
    }

    private StackPane buildUi() {
        totemPane.setAlignment(Pos.CENTER);
        confirmButton.setDisable(true);

        VBox center = new VBox(16, titleLabel, statusLabel, totemPane, confirmButton);
        center.setAlignment(Pos.TOP_CENTER);
        return Backgrounds.wrapInPanel(center, 900, "totem-selection-root");
    }

    private void wireActions() {
        confirmButton.setOnAction(e -> {
            if (pendingColor == null || submitting) return;
            try {
                submitting = true;
                confirmButton.setDisable(true);
                statusLabel.setText("Confirming " + pendingColor + "…");
                app.getSession().getVirtualServer().chooseTotem(pendingColor);
            } catch (RuntimeException ex) {
                submitting = false;
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });
    }

    private void render(TotemSelectionStateDTO state) {
        // A fresh state arrived from the server: clear the submitting latch.
        submitting = false;

        String me = app.getSession().getLocalNickname();
        String chooser = state.currentChooser();
        boolean myTurn = me != null && me.equalsIgnoreCase(chooser);

        // Drop a stale pending pick (no longer my turn, or already taken).
        if (!myTurn) {
            pendingColor = null;
        }

        if (myTurn) {
            statusLabel.setText("It's your turn — pick a totem and press Confirm.");
        } else {
            statusLabel.setText("Waiting for other players — "
                    + (chooser == null ? "…" : chooser) + " is choosing…");
        }

        totemPane.getChildren().clear();
        for (TotemOptionDTO option : state.options()) {
            boolean taken = option.ownerNickname() != null;
            if (taken && option.color().equalsIgnoreCase(stringOrEmpty(pendingColor))) {
                pendingColor = null; // someone took the color we had pending
            }
            totemPane.getChildren().add(buildTotemTile(option, myTurn));
        }

        confirmButton.setDisable(!(myTurn && pendingColor != null));
    }

    private Node buildTotemTile(TotemOptionDTO option, boolean myTurn) {
        boolean taken = option.ownerNickname() != null;
        boolean selectable = myTurn && !taken;
        boolean selected = option.color().equalsIgnoreCase(stringOrEmpty(pendingColor));

        Node image = ImageCache.totemNode(option.color(), TOTEM_SIZE, option.color());
        StackPane imageHolder = new StackPane(image);
        imageHolder.setPrefSize(TOTEM_SIZE, TOTEM_SIZE);

        if (taken) {
            // Dim a taken totem and overlay the owner's nickname on the photo.
            image.setOpacity(0.45);
            Label owner = new Label(option.ownerNickname());
            owner.setTextFill(Color.WHITE);
            owner.setPadding(new Insets(3, 8, 3, 8));
            owner.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 0, 0, 0.65), new CornerRadii(6), Insets.EMPTY)));
            StackPane.setAlignment(owner, Pos.BOTTOM_CENTER);
            imageHolder.getChildren().add(owner);
        }

        if (selected) {
            // "Luminous" highlight on the currently clicked totem.
            DropShadow glow = new DropShadow();
            glow.setColor(Color.GOLD);
            glow.setRadius(28);
            glow.setSpread(0.55);
            imageHolder.setEffect(glow);
        }

        Label colorLabel = new Label(option.color() + (taken ? " (taken)" : ""));
        VBox tile = new VBox(6, imageHolder, colorLabel);
        tile.setAlignment(Pos.CENTER);
        tile.setPadding(new Insets(8));

        if (selectable) {
            tile.setStyle("-fx-cursor: hand;");
            tile.setOnMouseClicked(e -> {
                pendingColor = option.color();
                // Re-render from the last known state to refresh highlight/button.
                TotemSelectionStateDTO latest = app.getSession().getLatestTotemSelectionState();
                if (latest != null) render(latest);
            });
        }

        return tile;
    }

    private static String stringOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
