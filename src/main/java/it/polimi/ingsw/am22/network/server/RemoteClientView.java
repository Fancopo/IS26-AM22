package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteClientView extends Remote {
    void receive(ServerMessage message) throws RemoteException;
}
