package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketClientHandler implements ClientChannel, Runnable {
    private final Socket socket;
    private final NetworkGameService gameService;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile String boundNickname;
    private volatile boolean closed;

    public SocketClientHandler(Socket socket, NetworkGameService gameService) throws IOException {
        this.socket = socket;
        this.gameService = gameService;
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.boundNickname = null;
        this.closed = false;
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                Object incoming = inputStream.readObject();
                if (incoming instanceof ClientRequest request) {
                    gameService.handleRequest(request, this);
                } else {
                    send(new ErrorMessage("Invalid payload received."));
                }
            }
        } catch (EOFException ignored) {
            if (!closed) {
                gameService.handleTransportDrop(this);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!closed) {
                gameService.handleTransportDrop(this);
            }
        } finally {
            close();
        }
    }

    @Override
    public synchronized void send(ServerMessage message) {
        if (closed) {
            return;
        }
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException e) {
            close();
            throw new IllegalStateException("Unable to send a message to the socket client.", e);
        }
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

    @Override
    public String getBoundNickname() {
        return boundNickname;
    }

    @Override
    public void setBoundNickname(String nickname) {
        this.boundNickname = nickname;
    }
}
