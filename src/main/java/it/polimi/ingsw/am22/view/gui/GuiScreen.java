package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import javafx.scene.Parent;

/**
 * Contratto comune delle schermate JavaFX.
 *
 * <p>{@link GuiApp} dialoga solo tramite questa interfaccia: produce un
 * {@link Parent} da montare nello stage e riceve i {@link ServerMessage}
 * (sempre sul JavaFX thread) per aggiornare i propri nodi.
 */
public interface GuiScreen {

    /** @return nodo radice da inserire nella Scene */
    Parent getRoot();

    /**
     * Invocato sul JavaFX thread ad ogni messaggio del server.
     * L'implementazione tipica filtra il tipo di messaggio di proprio interesse.
     *
     * @param message messaggio ricevuto
     */
    default void onServerMessage(ServerMessage message) {
    }
}
