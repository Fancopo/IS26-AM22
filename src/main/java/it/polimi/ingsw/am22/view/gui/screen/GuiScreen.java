package it.polimi.ingsw.am22.view.gui.screen;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.view.gui.GuiApp;
import javafx.scene.Parent;

/**
 * Contract for JavaFX screens. {@link GuiApp} only sees screens through this
 * interface: it mounts {@link #getRoot()} on the stage and forwards server
 * messages on the JavaFX thread.
 */
public interface GuiScreen {

    Parent getRoot();

    /** Invoked on the JavaFX thread for every incoming server message. */
    default void onServerMessage(ServerMessage message) {
    }

    /**
     * True for the in-match screen. {@link GuiApp} uses it to skip redundant
     * navigation when a game-state message arrives and we're already there.
     */
    default boolean isGameScreen() { return false; }

    /**
     * True for screens shown before joining a lobby (Nickname / Matches).
     * {@link GuiApp} uses it to decide whether a lobby/match-joined event
     * should push the Lobby screen.
     */
    default boolean isPreLobbyScreen() { return false; }

    /**
     * True for the crash-recovery waiting lobby. {@link GuiApp} uses it to
     * avoid rebuilding the screen on every reconnection update.
     */
    default boolean isRecoveryScreen() { return false; }
}
