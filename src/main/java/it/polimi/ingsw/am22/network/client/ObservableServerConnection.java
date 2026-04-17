package it.polimi.ingsw.am22.network.client;

public interface ObservableServerConnection extends ServerConnection, AutoCloseable {
    void setClientUpdateHandler(ClientUpdateHandler handler);

    @Override
    void close();
}
