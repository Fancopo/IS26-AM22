package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.controller.server.MatchManager;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RMI implementation of {@link RmiServerInterface}. Each submitRequest wraps the
 * client callback in a {@link RmiClientHandler} and delegates to the match manager.
 *
 * <p>Also drives a server-side liveness probe that periodically pings every
 * bound RMI client. RMI gives the server no notification when a client dies
 * (no read loop, no socket EOF), so without this a dead client would only be
 * discovered the next time the server happened to send to it — leaving the
 * match in a zombie state until then. Socket clients get this for free from
 * EOFException on their reader thread.
 */
public class RmiServer extends UnicastRemoteObject implements RmiServerInterface {

    /** How often the server pings each bound RMI client to detect silent drops. */
    private static final long LIVENESS_PROBE_INTERVAL_MS = 1000;

    private final MatchManager matchManager;

    public RmiServer(MatchManager matchManager) throws RemoteException {
        super();
        this.matchManager = matchManager;
    }

    @Override
    public void submitRequest(ClientRequest request, RmiClientInterface clientView) throws RemoteException {
        matchManager.handleRequest(request, new RmiClientHandler(clientView, matchManager));
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
        ScheduledExecutorService probe = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rmi-server-liveness-probe");
            t.setDaemon(true);
            return t;
        });
        probe.scheduleWithFixedDelay(matchManager::probeRmiClients,
                LIVENESS_PROBE_INTERVAL_MS, LIVENESS_PROBE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        return new Handle(registry, endpoint, bindingName, probe);
    }

    /**
     * Holds the references needed to tear down an RMI publication: without
     * them the registry and endpoint stay exported for the lifetime of the
     * JVM, blocking the port and preventing in-process restarts. Also owns
     * the liveness-probe executor so it gets cancelled with the server.
     */
    public static final class Handle {
        private final Registry registry;
        private final RmiServer endpoint;
        private final String bindingName;
        private final ScheduledExecutorService livenessProbe;

        private Handle(Registry registry, RmiServer endpoint, String bindingName,
                       ScheduledExecutorService livenessProbe) {
            this.registry = registry;
            this.endpoint = endpoint;
            this.bindingName = bindingName;
            this.livenessProbe = livenessProbe;
        }

        /** Best-effort release: unbind + unexport endpoint + unexport registry. Idempotent. */
        public void shutdown() {
            try { livenessProbe.shutdownNow(); } catch (Exception ignored) {}
            try { registry.unbind(bindingName); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(endpoint, true); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) {}
        }
    }
}
