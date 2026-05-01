package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.server.NetworkGameService;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Implementazione RMI del {@link RemoteGameServer}.
 *
 * Estende UnicastRemoteObject per essere direttamente esportabile
 * come oggetto remoto. Ogni chiamata a submitRequest crea un
 * RmiClientChannel sopra il callback del client e delega la
 * gestione al NetworkGameService.
 */
public class RmiGameServer extends UnicastRemoteObject implements RemoteGameServer {
    private final NetworkGameService gameService;

    /**
     * Costruisce ed esporta il server RMI.
     *
     * @param gameService servizio a cui inoltrare le richieste
     * @throws RemoteException se l'export RMI fallisce
     */
    public RmiGameServer(NetworkGameService gameService) throws RemoteException {
        super();
        this.gameService = gameService;
    }

    @Override
    public void submitRequest(ClientRequest request, RemoteClientView clientView) throws RemoteException {
        gameService.handleRequest(request, new RmiClientChannel(clientView));
    }

    /**
     * Metodo di utilità: crea un registry sulla porta indicata e vi registra
     * un nuovo {@code RmiGameServer} con il binding fornito.
     *
     * @param port        porta del registry RMI
     * @param bindingName nome del binding (es. {@code MESOS_SERVER})
     * @param gameService servizio da esporre
     * @return il {@link Registry} creato
     * @throws RemoteException       se la creazione/registrazione fallisce
     * @throws AlreadyBoundException se il binding esiste già
     */
    public static Registry publish(int port, String bindingName, NetworkGameService gameService)
            throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind(bindingName, new RmiGameServer(gameService));
        return registry;
    }
}
