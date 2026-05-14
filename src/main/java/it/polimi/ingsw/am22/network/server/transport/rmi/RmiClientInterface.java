package it.polimi.ingsw.am22.network.server.transport.rmi;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback implemented by the client. The client exports its
 * implementation and passes it to {@code submitRequest}; the server uses it
 * via {@link RmiClientChannel} to deliver replies.
 */
public interface RmiClientInterface extends Remote {
    void receive(ServerMessage message) throws RemoteException;
}
