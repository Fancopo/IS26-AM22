package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.controller.server.MatchManager;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * RMI implementation of {@link RmiServerInterface}. Each submitRequest wraps the
 * client callback in a {@link RmiClientHandler} and delegates to the match manager.
 */
public class RmiServer extends UnicastRemoteObject implements RmiServerInterface {
    private final MatchManager matchManager;

    public RmiServer(MatchManager matchManager) throws RemoteException {
        super();
        this.matchManager = matchManager;
    }

    @Override
    public void submitRequest(ClientRequest request, RmiClientInterface clientView) throws RemoteException {
        matchManager.handleRequest(request, new RmiClientHandler(clientView));
    }

    @Override
    public void ping() {
        // No-op: the signal is the RemoteException the client gets when the server is gone.
    }

    /**
     * Creates an RMI registry on {@code port} and binds a fresh server under
     * {@code bindingName}. The returned {@link Handle} keeps the registry and
     * endpoint references alive and exposes {@link Handle#shutdown()} for a
     * graceful tear-down (unbind + unexport, releasing the port).
     */
    public static Handle publish(int port, String bindingName, MatchManager matchManager)
            throws RemoteException, AlreadyBoundException {
        Registry registry = LocateRegistry.createRegistry(port);
        RmiServer endpoint = new RmiServer(matchManager);
        registry.bind(bindingName, endpoint);
        return new Handle(registry, endpoint, bindingName);
    }

    /**
     * Holds the references needed to tear down an RMI publication: without
     * them the registry and endpoint stay exported for the lifetime of the
     * JVM, blocking the port and preventing in-process restarts.
     */
    public static final class Handle {
        private final Registry registry;
        private final RmiServer endpoint;
        private final String bindingName;

        private Handle(Registry registry, RmiServer endpoint, String bindingName) {
            this.registry = registry;
            this.endpoint = endpoint;
            this.bindingName = bindingName;
        }

        /** Best-effort release: unbind + unexport endpoint + unexport registry. Idempotent. */
        public void shutdown() {
            try { registry.unbind(bindingName); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(endpoint, true); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) {}
        }
    }
}
