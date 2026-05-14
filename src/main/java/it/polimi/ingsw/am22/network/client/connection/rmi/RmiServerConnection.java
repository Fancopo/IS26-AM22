package it.polimi.ingsw.am22.network.client.connection.rmi;

import it.polimi.ingsw.am22.network.client.ClientUpdateHandler;
import it.polimi.ingsw.am22.network.client.connection.ObservableServerConnection;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.server.transport.rmi.RemoteClientView;
import it.polimi.ingsw.am22.network.server.transport.rmi.RemoteGameServer;

import java.io.Serial;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * RMI-based {@link ObservableServerConnection}.
 *
 * <p>On construction looks up the remote {@link RemoteGameServer} stub and
 * exports a local callback ({@link ClientCallback}) that the server invokes
 * to deliver messages. No reader thread is needed: the RMI runtime drives
 * the callback.
 */
public class RmiServerConnection implements ObservableServerConnection {

    private static final long PING_INTERVAL_MS = 1000;

    /**
     * Delay after a farewell message (EndGame / MatchClosed) before we
     * synthesize a local disconnect event. RMI doesn't emit disconnect
     * events on its own, so without this the client would exit silently
     * (the callback stub stays alive and the server is still pingable —
     * it just stopped talking to us). Kept low so "[CONN] Server connection
     * lost" reaches the TUI while it's still blocked on stdin.
     */
    private static final long FAREWELL_GRACE_MS = 200;

    private final RemoteGameServer remoteGameServer;
    private final RemoteClientView callback;
    private final ScheduledExecutorService livenessProbe;
    private volatile ClientUpdateHandler updateHandler;
    private volatile boolean closed;

    public RmiServerConnection(String host, int port, String bindingName) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(Objects.requireNonNull(host, "host cannot be null"), port);
        this.remoteGameServer = (RemoteGameServer) registry.lookup(bindingName);
        this.callback = new ClientCallback();
        // RMI doesn't notify us when the server dies: we only find out when a
        // remote call throws. Periodic ping makes that detection bounded by
        // PING_INTERVAL_MS even when the user isn't interacting. Socket
        // transport gets this for free from EOFException on the reader thread.
        this.livenessProbe = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rmi-liveness-probe");
            t.setDaemon(true);
            return t;
        });
        livenessProbe.scheduleWithFixedDelay(this::probe,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void probe() {
        if (closed) return;
        try {
            remoteGameServer.ping();
        } catch (RemoteException e) {
            fireConnectionClosed(e);
        }
    }

    /** Idempotent: avoids duplicate notifications when probe and farewell race. */
    private void fireConnectionClosed(Throwable cause) {
        if (closed) return;
        ClientUpdateHandler handler = updateHandler;
        close();
        if (handler != null) {
            handler.onConnectionClosed(cause);
        }
    }

    @Override
    public void setClientUpdateHandler(ClientUpdateHandler handler) {
        this.updateHandler = handler;
    }

    @Override
    public void listMatches() {
        send(new ListMatchesRequest());
    }

    @Override
    public void createMatch(String hostNickname, int expectedPlayers) {
        send(new CreateMatchRequest(hostNickname, expectedPlayers));
    }

    @Override
    public void addPlayerToLobby(String matchId, String nickname) {
        send(new AddPlayerToLobbyRequest(matchId, nickname));
    }

    @Override
    public void setExpectedPlayers(String matchId, String requesterNickname, int expectedPlayers) {
        send(new SetExpectedPlayersRequest(matchId, requesterNickname, expectedPlayers));
    }

    @Override
    public void removePlayerFromLobby(String matchId, String nickname) {
        send(new RemovePlayerFromLobbyRequest(matchId, nickname));
    }

    @Override
    public void placeTotem(String matchId, String playerNickname, char offerLetter) {
        send(new PlaceTotemRequest(matchId, playerNickname, offerLetter));
    }

    @Override
    public void pickCards(String matchId, String playerNickname, List<String> selectedCardIds) {
        send(new PickCardsRequest(matchId, playerNickname, selectedCardIds));
    }

    @Override
    public void pickBonusCard(String matchId, String playerNickname, String bonusCardId) {
        send(new PickBonusCardRequest(matchId, playerNickname, bonusCardId));
    }

    @Override
    public void disconnectPlayer(String matchId, String nickname) {
        send(new DisconnectPlayerRequest(matchId, nickname));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        livenessProbe.shutdownNow();
        try {
            UnicastRemoteObject.unexportObject(callback, true);
        } catch (Exception ignored) {
        }
    }

    private void send(it.polimi.ingsw.am22.network.common.message.ClientRequest request) {
        if (closed) {
            throw new IllegalStateException("The RMI connection is closed.");
        }
        try {
            remoteGameServer.submitRequest(request, callback);
        } catch (RemoteException e) {
            close();
            throw new IllegalStateException("Unable to invoke the remote server.", e);
        }
    }

    /** RMI-exported callback the server invokes to deliver messages to this client. */
    private final class ClientCallback extends UnicastRemoteObject implements RemoteClientView {
        @Serial
        private static final long serialVersionUID = 1L;

        private ClientCallback() throws RemoteException {
            super();
        }

        @Override
        public void receive(ServerMessage message) throws RemoteException {
            ClientUpdateHandler handler = updateHandler;
            if (handler != null) {
                handler.onServerMessage(message);
            }
            // EndGame / MatchClosed are the last messages the server will send
            // to this client; afterwards it simply stops calling the callback
            // (RmiClientChannel.close() server-side is a no-op). Without a
            // synthetic disconnect the RMI client would exit silently, so we
            // give the TUI/GUI the same event the socket transport produces.
            if (message instanceof EndGameMessage || message instanceof MatchClosedMessage) {
                if (!closed) {
                    livenessProbe.schedule(
                            () -> fireConnectionClosed(null),
                            FAREWELL_GRACE_MS, TimeUnit.MILLISECONDS);
                }
            }
        }
    }
}
