package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.server.NetworkGameService;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI implementation of {@link RemoteGameServer}. Each submitRequest wraps the
 * client callback in a {@link RmiClientChannel} and delegates to the service.
 */
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

    @Override
    public void ping() {
        // No-op: the signal is the RemoteException the client gets when the server is gone.
    }

    /** Creates an RMI registry on {@code port} and binds a fresh server under {@code bindingName}. */
    public static Registry publish(int port, String bindingName, NetworkGameService gameService)
            throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind(bindingName, new RmiGameServer(gameService));
        return registry;
    }
}
