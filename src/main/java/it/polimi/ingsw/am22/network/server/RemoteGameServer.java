package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteGameServer extends Remote {
    void submitRequest(ClientRequest request, RemoteClientView clientView) throws RemoteException;
}
