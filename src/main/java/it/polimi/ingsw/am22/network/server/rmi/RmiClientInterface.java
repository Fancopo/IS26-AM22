package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI callback implemented by the client. The client exports its
 * implementation and passes it to {@code submitRequest}; the server uses it
 * via {@link RmiClientHandler} to deliver replies.
 */
public interface RmiClientInterface extends Remote {
    void receive(ServerMessage message) throws RemoteException;

    /**
     * No-op liveness probe. The server periodically invokes this to detect
     * RMI clients that have died without a clean disconnect — the
     * RemoteException thrown when the stub is unreachable is the signal.
     * Socket transport gets the same detection for free from EOF on the
     * read loop; RMI has no equivalent server-side reader.
     */
    void ping() throws RemoteException;
}
