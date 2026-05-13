package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/** RMI interface the server exposes to clients. Concrete impl: {@link RmiGameServer}. */
public interface RemoteGameServer extends Remote {

    /** Forwards a request and registers the callback used to deliver replies. */
    void submitRequest(ClientRequest request, RemoteClientView clientView) throws RemoteException;

    /**
     * No-op the client calls periodically to detect a dead server: RMI doesn't
     * emit disconnect events, the only signal is a failed call.
     */
    void ping() throws RemoteException;
}
