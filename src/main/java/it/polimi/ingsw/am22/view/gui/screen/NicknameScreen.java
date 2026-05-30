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

/** Nickname picker. Only listens for {@link ErrorMessage}; navigation is driven by {@link GuiApp}. */
public final class NicknameScreen implements GuiScreen {

    @Override public boolean isPreLobbyScreen() { return true; }

    private final GuiApp app;
    private final StackPane root;
    private final TextField nicknameField;
    private final Label statusLabel;
    private final Button joinButton;

    public NicknameScreen(GuiApp app) {
        this.app = app;
        this.nicknameField = new TextField();
        this.statusLabel = new Label();
        this.joinButton = new Button("Continue");
        this.root = buildUi();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(ErrorMessage m) { statusLabel.setText(m.message()); }
        });
    }

    private StackPane buildUi() {
        nicknameField.setPromptText("Your nickname");

        joinButton.setOnAction(e -> {
            String nickname = nicknameField.getText() == null ? "" : nicknameField.getText().trim();
            if (nickname.isEmpty()) {
                statusLabel.setText("Nickname cannot be empty.");
                return;
            }
            // Server binding happens only when the user picks/creates a match.
            app.showMatchesScreen(nickname);
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            // Going back to the connection screen closes the current session.
            if (app.getSession() != null) {
                app.getSession().close(false);
            }
            app.showConnectionScreen();
        });

        VBox box = new VBox(14,
                new Label("Choose your nickname"),
                nicknameField,
                joinButton,
                backButton,
                statusLabel);
        box.setAlignment(Pos.CENTER);
        return Backgrounds.wrapInPanel(box, 420, "nickname-root");
    }
}
