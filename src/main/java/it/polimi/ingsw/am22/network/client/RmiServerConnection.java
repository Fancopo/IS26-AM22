package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.server.RemoteClientView;
import it.polimi.ingsw.am22.network.server.RemoteGameServer;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Objects;

public class RmiServerConnection implements ObservableServerConnection {
    private final RemoteGameServer remoteGameServer;
    private final RemoteClientView callback;
    private volatile ClientUpdateHandler updateHandler;
    private volatile boolean closed;

    public RmiServerConnection(String host, int port, String bindingName) throws RemoteException, NotBoundException {
        Registry registry = LocateRegistry.getRegistry(Objects.requireNonNull(host, "host cannot be null"), port);
        this.remoteGameServer = (RemoteGameServer) registry.lookup(bindingName);
        this.updateHandler = null;
        this.closed = false;
        this.callback = new ClientCallback();
    }

    @Override
    public void setClientUpdateHandler(ClientUpdateHandler handler) {
        this.updateHandler = handler;
    }

    @Override
    public void addPlayerToLobby(String nickname) {
        send(new AddPlayerToLobbyRequest(nickname));
    }

    @Override
    public void setExpectedPlayers(String requesterNickname, int expectedPlayers) {
        send(new SetExpectedPlayersRequest(requesterNickname, expectedPlayers));
    }

    @Override
    public void removePlayerFromLobby(String nickname) {
        send(new RemovePlayerFromLobbyRequest(nickname));
    }

    @Override
    public void placeTotem(String playerNickname, char offerLetter) {
        send(new PlaceTotemRequest(playerNickname, offerLetter));
    }

    @Override
    public void pickCards(String playerNickname, List<String> selectedCardIds) {
        send(new PickCardsRequest(playerNickname, selectedCardIds));
    }

    @Override
    public void pickBonusCard(String playerNickname, String bonusCardId) {
        send(new PickBonusCardRequest(playerNickname, bonusCardId));
    }

    @Override
    public void disconnectPlayer(String nickname) {
        send(new DisconnectPlayerRequest(nickname));
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
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

    private final class ClientCallback extends UnicastRemoteObject implements RemoteClientView {
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
