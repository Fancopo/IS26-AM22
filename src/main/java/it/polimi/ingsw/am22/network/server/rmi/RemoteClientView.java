package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback implemented by the client. The client exports its
 * implementation and passes it to {@code submitRequest}; the server uses it
 * via {@link RmiClientChannel} to deliver replies.
 */
public interface RemoteClientView extends Remote {
    void receive(ServerMessage message) throws RemoteException;
}
