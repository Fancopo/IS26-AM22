package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;

/**
 * Callback the client view implements to receive asynchronous updates.
 *
 * <p>Invoked from the reader thread (socket) or an RMI thread: implementations
 * must be thread-safe or marshal updates onto the proper UI thread (Swing EDT
 * or JavaFX application thread).
 */
public interface ServerMessageDispatcher {

    void onServerMessage(ServerMessage message);

    /** @param cause null on a clean close. */
    default void onConnectionClosed(Throwable cause) {}
}
