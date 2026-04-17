package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class RmiGameServer extends UnicastRemoteObject implements RemoteGameServer {
    private final NetworkGameService gameService;

    public RmiGameServer(NetworkGameService gameService) throws RemoteException {
        super();
        this.gameService = gameService;
    }

    @Override
    public void submitRequest(ClientRequest request, RemoteClientView clientView) throws RemoteException {
        gameService.handleRequest(request, new RmiClientChannel(clientView));
    }

    public static Registry publish(int port, String bindingName, NetworkGameService gameService)
            throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind(bindingName, new RmiGameServer(gameService));
        return registry;
    }
}
