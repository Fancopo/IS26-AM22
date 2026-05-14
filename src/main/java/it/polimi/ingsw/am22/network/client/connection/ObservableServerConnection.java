package it.polimi.ingsw.am22.network.client.connection;

import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;

/**
 * {@link ServerConnection} that also delivers asynchronous server messages
 * to a registered handler. AutoCloseable so concrete transports can be used
 * with try-with-resources.
 */
public interface ObservableServerConnection extends ServerConnection, AutoCloseable {

    /** Registers the handler invoked on every incoming server message. */
    void setClientUpdateHandler(ClientUpdateHandler handler);

    @Override
    void close();
}
