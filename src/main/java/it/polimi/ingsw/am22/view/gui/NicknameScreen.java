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

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Schermata di inserimento nickname.
 *
 * <p>Flusso:
 * <ol>
 *     <li>utente digita il nickname e preme Join;</li>
 *     <li>il {@code ClientController} invia {@code AddPlayerToLobbyRequest};</li>
 *     <li>se il server risponde con {@link LobbyStateMessage} contenente il
 *         nickname, {@link GuiApp} passa alla {@link LobbyScreen};</li>
 *     <li>se arriva {@link ErrorMessage} (nickname duplicato, invalido, ...)
 *         viene mostrato e il pulsante torna attivo.</li>
 * </ol>
 */
public final class NicknameScreen implements GuiScreen {

    private final GuiApp app;
    private final StackPane root;
    private final TextField nicknameField;
    private final Label statusLabel;
    private final Button joinButton;

    /** {@code true} dopo che l'utente ha premuto Join, finché non arriva lobby/errore. */
    private boolean pendingJoin;

    public NicknameScreen(GuiApp app) {
        this.app = app;
        this.nicknameField = new TextField();
        this.statusLabel = new Label();
        this.joinButton = new Button("Join");
        this.root = buildUi();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    /**
     * Segnala a {@link GuiApp} se la prossima {@link LobbyStateMessage} che
     * arriva deve triggerare la navigazione automatica verso la lobby.
     */
    public boolean hasPendingJoin() {
        return pendingJoin;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(ErrorMessage m) {
                if (pendingJoin) {
                    pendingJoin = false;
                    statusLabel.setText(m.message());
                    joinButton.setDisable(false);
                }
            }
            @Override public void visit(LobbyStateMessage m) { pendingJoin = false; }
            @Override public void visit(GameStartedMessage m) {}
            @Override public void visit(GameStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(InfoMessage m) {}
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
            statusLabel.setText("Joining lobby...");
            joinButton.setDisable(true);
            pendingJoin = true;
            try {
                app.getSession().getClientController().addPlayerToLobby(nickname);
            } catch (RuntimeException ex) {
                pendingJoin = false;
                statusLabel.setText("Join failed: " + ex.getMessage());
                joinButton.setDisable(false);
            }
        });

        VBox box = new VBox(14,
                new Label("Choose your nickname"),
                nicknameField,
                joinButton,
                statusLabel);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(40));

        // GRAPHIC PLACEHOLDER: sfondo da sostituire/aggiungere.
        StackPane container = new StackPane(box);
        container.setId("nickname-root");
        return container;
    }
}
