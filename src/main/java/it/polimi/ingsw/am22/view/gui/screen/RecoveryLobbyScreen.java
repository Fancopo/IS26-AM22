package it.polimi.ingsw.am22.view.gui.screen;

import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.protocol.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchRecoveringMessage;

import it.polimi.ingsw.am22.view.gui.GuiApp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Lobby-style screen shown after reconnecting to a crash-recovered match that
 * is still waiting for its other players. Lists every player of the suspended
 * match marking who is back and who is still missing; the game resumes
 * (→ GameScreen) automatically once everyone has returned.
 */
public final class RecoveryLobbyScreen implements GuiScreen {

    @Override public boolean isRecoveryScreen() { return true; }

    private final GuiApp app;
    private final StackPane root;

    private final Label titleLabel = new Label();
    private final Label statusLabel = new Label();
    private final ObservableList<String> playerItems = FXCollections.observableArrayList();
    private final ListView<String> playerList = new ListView<>(playerItems);
    private final Button leaveButton = new Button("Leave match");

    public RecoveryLobbyScreen(GuiApp app) {
        this.app = app;
        this.root = buildUi();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(MatchRecoveringMessage m) { render(m); }
        });
    }

    private StackPane buildUi() {
        String matchId = app.getSession() == null ? null
                : app.getSession().getVirtualServer().getMatchId();
        titleLabel.setText("Resuming match" + (matchId == null ? "" : " " + matchId));

        leaveButton.setOnAction(e -> {
            String me = app.getSession() == null ? null : app.getSession().getLocalNickname();
            app.leaveMatchAndShowMatches(me);
        });

        VBox center = new VBox(14,
                titleLabel,
                new Label("Players in this match:"),
                playerList,
                statusLabel,
                leaveButton);
        center.setAlignment(Pos.TOP_CENTER);
        return Backgrounds.wrapInPanel(center, 560, "lobby-root");
    }

    /** Re-renders the player list and waiting status from a recovery update. */
    private void render(MatchRecoveringMessage m) {
        Set<String> missing = new HashSet<>();
        if (m.missingNicknames() != null) {
            for (String n : m.missingNicknames()) {
                missing.add(n.toLowerCase(Locale.ROOT));
            }
        }
        playerItems.clear();
        GameStateDTO state = m.gameState();
        if (state != null && state.players() != null) {
            for (PlayerDTO p : state.players()) {
                boolean back = !missing.contains(p.nickname().toLowerCase(Locale.ROOT));
                playerItems.add(p.nickname() + (back ? "  — reconnected" : "  — waiting…"));
            }
        }
        statusLabel.setText("Waiting for all players to reconnect ("
                + m.reconnectedCount() + "/" + m.totalPlayers() + "). "
                + "The game resumes automatically once everyone is back.");
    }
}
