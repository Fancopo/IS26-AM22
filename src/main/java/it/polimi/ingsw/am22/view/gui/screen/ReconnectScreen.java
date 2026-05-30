package it.polimi.ingsw.am22.view.gui.screen;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;

import it.polimi.ingsw.am22.view.gui.GuiApp;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Shown only when the connection dropped while the player was in a running
 * match — i.e. the previous match was closed by a server crash. The player
 * first chooses to reconnect, then types the nickname they used in that
 * match; the server validates it against the suspended match's players and a
 * mismatch lets the player correct it and try again.
 */
public final class ReconnectScreen implements GuiScreen {

    private final GuiApp app;
    private final String matchId;

    private final Label statusLabel = new Label();
    private final TextField nicknameField = new TextField();
    private final Button startButton = new Button("Reconnect to previous match");
    private final Button confirmButton = new Button("Confirm nickname");
    private final VBox nicknameBox;
    private final StackPane root;

    public ReconnectScreen(GuiApp app, String matchId) {
        this.app = app;
        this.matchId = matchId;
        this.nicknameBox = new VBox(8,
                new Label("Enter the nickname you used in match " + matchId + ":"),
                nicknameField,
                confirmButton);
        this.nicknameBox.setAlignment(Pos.CENTER);
        this.root = buildUi();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(ErrorMessage m) {
                // Reconnection refused (e.g. nickname mismatch): keep the field
                // editable so the player can correct it and try again.
                statusLabel.setText(m.message());
                confirmButton.setDisable(false);
                nicknameField.setDisable(false);
            }
        });
    }

    private StackPane buildUi() {
        Label title = new Label("Server connection lost");
        Label info = new Label(
                "The server went down during your match (" + matchId + ").\n"
                + "The match was saved and can be resumed.");
        info.setWrapText(true);

        nicknameField.setPromptText("nickname used in that match");

        // Phase 1: only the reconnect choice is shown; the nickname prompt
        // appears after the player commits to reconnecting.
        nicknameBox.setVisible(false);
        nicknameBox.setManaged(false);

        startButton.setOnAction(e -> {
            startButton.setVisible(false);
            startButton.setManaged(false);
            nicknameBox.setVisible(true);
            nicknameBox.setManaged(true);
            statusLabel.setText("");
            nicknameField.requestFocus();
        });

        confirmButton.setOnAction(e -> {
            String nickname = nicknameField.getText() == null ? "" : nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                statusLabel.setText("Nickname cannot be empty.");
                return;
            }
            statusLabel.setText("Reconnecting…");
            confirmButton.setDisable(true);
            nicknameField.setDisable(true);
            boolean ok = app.reconnectToPreviousMatch(matchId, nickname);
            if (!ok) {
                statusLabel.setText("Server still unreachable — try again.");
                confirmButton.setDisable(false);
                nicknameField.setDisable(false);
            }
        });

        Button backButton = new Button("Leave this match");
        // Leaving deletes the suspended match server-side and notifies the other
        // players that it is over. The leaving player keeps the connection they
        // first chose and lands back on the nickname scene, ready to play again.
        backButton.setOnAction(e -> app.leavePreviousMatchAndShowNickname(matchId));

        VBox box = new VBox(16, title, info, startButton, nicknameBox, backButton, statusLabel);
        box.setAlignment(Pos.CENTER);
        return Backgrounds.wrapInPanel(box, 460, "reconnect-root");
    }
}
