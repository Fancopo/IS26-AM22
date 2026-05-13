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

/**
 * TCP socket {@link ObservableServerConnection}. A daemon reader thread reads
 * incoming {@link ServerMessage}s and forwards them to the registered handler.
 * {@link #send} is synchronized to keep writes on the shared output stream atomic.
 */
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
        this.readerThread = new Thread(this::readLoop, "socket-server-connection-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
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
