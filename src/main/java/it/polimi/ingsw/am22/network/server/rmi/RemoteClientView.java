package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI implementata dal client per ricevere i messaggi dal server.
 *
 * Il client esporta una sua implementazione ({@code ClientCallback}) e la
 * passa al server ad ogni {@code submitRequest}: il server la usa poi
 * tramite {@link RmiClientChannel} per recapitare le risposte.
 */
public interface RemoteClientView extends Remote {

    /**
     * Invocato dal server per consegnare un messaggio al client.
     *
     * @param message messaggio da consegnare
     * @throws RemoteException se il recapito via RMI fallisce
     */
    void receive(ServerMessage message) throws RemoteException;
}
