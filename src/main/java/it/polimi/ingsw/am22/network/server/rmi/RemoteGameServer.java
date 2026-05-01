package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Interfaccia RMI esposta dal server ai client.
 *
 * L'implementazione concreta è {@link RmiGameServer}, pubblicata sul
 * registry RMI con un nome noto (es. {@code MESOS_SERVER}).
 */
public interface RemoteGameServer extends Remote {

    /**
     * Inoltra una richiesta al server e fornisce il callback per la risposta.
     *
     * @param request    richiesta del client
     * @param clientView callback esportato dal client per ricevere i messaggi del server
     * @throws RemoteException in caso di errore di trasporto RMI
     */
    void submitRequest(ClientRequest request, RemoteClientView clientView) throws RemoteException;
}
