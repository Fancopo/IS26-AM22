package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.request.*;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.Objects;

public class SocketServerConnection implements ObservableServerConnection {
    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile ClientUpdateHandler updateHandler;
    private final Thread readerThread;
    private volatile boolean closed;

    public SocketServerConnection(String host, int port) throws IOException {
        this.socket = new Socket(Objects.requireNonNull(host, "host cannot be null"), port);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.updateHandler = null;
        this.closed = false;
        this.readerThread = new Thread(this::readLoop, "socket-server-connection-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
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
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private synchronized void send(ClientRequest request) {
        if (closed) {
            throw new IllegalStateException("The socket connection is closed.");
        }
        try {
            outputStream.writeObject(request);
            outputStream.flush();
        } catch (IOException e) {
            close();
            throw new IllegalStateException("Unable to send the request to the server.", e);
        }
    }

    private void readLoop() {
        Throwable cause = null;
        try {
            while (!closed) {
                Object incoming = inputStream.readObject();
                if (incoming instanceof ServerMessage message) {
                    ClientUpdateHandler handler = updateHandler;
                    if (handler != null) {
                        handler.onServerMessage(message);
                    }
                }
            }
        } catch (EOFException eof) {
            cause = eof;
        } catch (IOException | ClassNotFoundException e) {
            if (!closed) {
                cause = e;
            }
        } finally {
            close();
            ClientUpdateHandler handler = updateHandler;
            if (handler != null) {
                handler.onConnectionClosed(cause);
            }
        }
    }
}
