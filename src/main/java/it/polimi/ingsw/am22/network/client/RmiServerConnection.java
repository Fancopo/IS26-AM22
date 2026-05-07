package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.server.rmi.RemoteClientView;
import it.polimi.ingsw.am22.network.server.rmi.RemoteGameServer;

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
 * Implementazione di {@link ObservableServerConnection} basata su RMI.
 *
 * Al momento della costruzione fa lookup sul registry remoto per ottenere
 * lo stub di {@link RemoteGameServer} ed esporta un callback locale
 * ({@link ClientCallback}) che il server invocherà per recapitare i messaggi.
 * A differenza della versione socket non serve un reader thread: è il runtime
 * RMI a gestire le chiamate verso il callback.
 */
public class RmiServerConnection implements ObservableServerConnection {

    private static final long PING_INTERVAL_MS = 1000;

    private final RemoteGameServer remoteGameServer;
    private final RemoteClientView callback;
    private final ScheduledExecutorService livenessProbe;
    private volatile ClientUpdateHandler updateHandler;
    private volatile boolean closed;

    /**
     * Si connette al server RMI facendo lookup del binding indicato.
     *
     * @param host        indirizzo del registry
     * @param port        porta del registry
     * @param bindingName nome con cui il server è registrato
     * @throws RemoteException   se il registry non è raggiungibile
     * @throws NotBoundException se il binding non è presente nel registry
     */
    public RmiServerConnection(String host, int port, String bindingName) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(Objects.requireNonNull(host, "host cannot be null"), port);
        this.remoteGameServer = (RemoteGameServer) registry.lookup(bindingName);
        this.updateHandler = null;
        this.closed = false;
        this.callback = new ClientCallback();
        // RMI non emette eventi di disconnessione: ci accorgiamo che il server è
        // morto solo quando una chiamata remota fallisce. Schedulo un ping
        // periodico così che la morte venga rilevata entro PING_INTERVAL_MS
        // anche se il giocatore non sta interagendo. Il socket transport ottiene
        // la stessa cosa "gratis" dall'EOFException sul reader thread.
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
            ClientUpdateHandler handler = updateHandler;
            close();
            if (handler != null) {
                handler.onConnectionClosed(e);
            }
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

    /**
     * Chiude la connessione rimuovendo il callback dall'export RMI.
     * L'operazione è idempotente.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        livenessProbe.shutdownNow();
        try {
            UnicastRemoteObject.unexportObject(callback, true);
        } catch (Exception ignored) {
        }
    }

    /**
     * Inoltra una richiesta al server remoto passando il callback locale
     * come destinatario delle risposte.
     *
     * @param request richiesta da inviare
     * @throws IllegalStateException se la connessione è chiusa o se la chiamata RMI fallisce
     */
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

    /**
     * Oggetto esportato via RMI che il server invoca per recapitare i messaggi
     * al client locale. I messaggi vengono inoltrati all'{@link ClientUpdateHandler}.
     */
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
        }
    }
}
