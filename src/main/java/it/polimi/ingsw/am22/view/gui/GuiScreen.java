package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
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
}
