package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.controller.server.MatchManager;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.server.AsyncClientHandler;
import it.polimi.ingsw.am22.network.server.ClientHandler;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RMI implementation of {@link RmiServerInterface}. Each submitRequest is
 * dispatched on a server-side executor so the remote call returns immediately
 * — the client's {@code send()} stops being a synchronous round-trip and the
 * UI thread is never blocked on remote processing.
 *
 * <p>The {@link ClientHandler} used to deliver replies is wrapped in an
 * {@link AsyncClientHandler}, so a slow/unreachable RMI client never blocks
 * the broadcast loop. The wrapper is cached per remote callback stub: the
 * same client across multiple {@code submitRequest} calls keeps the same
 * handler instance, preserving {@code boundNickname} / {@code boundMatchId}.
 *
 * <p>Also drives a server-side liveness probe that periodically pings every
 * bound RMI client. RMI gives the server no notification when a client dies
 * (no read loop, no socket EOF), so without this a dead client would only be
 * discovered the next time the server happened to send to it — leaving the
 * match in a zombie state until then. Socket clients get this for free from
 * EOFException on their reader thread (with the application-level ping on
 * top, see {@code SocketClientHandler}).
 */
public class RmiServer extends UnicastRemoteObject implements RmiServerInterface {

    /** How often the server pings each bound RMI client to detect silent drops. */
    private static final long LIVENESS_PROBE_INTERVAL_MS = 1000;

    private final MatchManager matchManager;

    private final ExecutorService inbound;
    private final Map<RmiClientInterface, ClientHandler> handlerByCallback;

    public RmiServer(MatchManager matchManager) throws RemoteException {
        super();
        this.matchManager = matchManager;
        this.inbound = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "rmi-inbound-dispatch");
            t.setDaemon(true);
            return t;
        });
        this.handlerByCallback = new ConcurrentHashMap<>();
    }

    @Override
    public void submitRequest(ClientRequest request, RmiClientInterface clientView) {
        // The RMI call returns as soon as the request is enqueued; handling
        // and any resulting broadcast happen on the inbound executor.
        ClientHandler handler = handlerFor(clientView);
        inbound.execute(() -> matchManager.handleRequest(request, handler));
    }

    /**
     * Cached lookup of the handler bound to an RMI client. The first call for
     * a given callback stub creates the handler; subsequent calls return the
     * same instance, so {@code boundNickname} / {@code boundMatchId} survive
     * across requests from that client.
     */
    private ClientHandler handlerFor(RmiClientInterface clientView) {
        return handlerByCallback.computeIfAbsent(clientView, this::createHandlerFor);
    }

    private ClientHandler createHandlerFor(RmiClientInterface callback) {
        return new AsyncClientHandler(
                new RmiClientHandler(callback, matchManager),
                "rmi-" + System.identityHashCode(callback));
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
            try { endpoint.inbound.shutdownNow(); } catch (Exception ignored) {}
            try { registry.unbind(bindingName); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(endpoint, true); } catch (Exception ignored) {}
            try { UnicastRemoteObject.unexportObject(registry, true); } catch (Exception ignored) {}
        }
    }
}
