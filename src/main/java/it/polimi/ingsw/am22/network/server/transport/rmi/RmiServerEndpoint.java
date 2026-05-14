package it.polimi.ingsw.am22.network.server.transport.rmi;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.server.MatchManager;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI implementation of {@link RmiServerInterface}. Each submitRequest wraps the
 * client callback in a {@link RmiClientChannel} and delegates to the service.
 */
public class RmiServerEndpoint extends UnicastRemoteObject implements RmiServerInterface {
    private final MatchManager gameService;

    public RmiServerEndpoint(MatchManager gameService) throws RemoteException {
        super();
        this.gameService = gameService;
    }

    @Override
    public void submitRequest(ClientRequest request, RmiClientInterface clientView) throws RemoteException {
        gameService.handleRequest(request, new RmiClientChannel(clientView));
    }

    @Override
    public void ping() {
        // No-op: the signal is the RemoteException the client gets when the server is gone.
    }

    /** Creates an RMI registry on {@code port} and binds a fresh server under {@code bindingName}. */
    public static Registry publish(int port, String bindingName, MatchManager gameService)
            throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(port);
        registry.bind(bindingName, new RmiServerEndpoint(gameService));
        return registry;
    }
}
