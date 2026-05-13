package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Nickname picker. Only listens for {@link ErrorMessage}; navigation is driven by {@link GuiApp}. */
public final class NicknameScreen implements GuiScreen {

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
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(InfoMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
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
        Backgrounds.stylePanel(box);
        box.setMaxWidth(420);
        box.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane(box);
        container.setId("nickname-root");
        Backgrounds.install(container);
        return container;
    }
}
