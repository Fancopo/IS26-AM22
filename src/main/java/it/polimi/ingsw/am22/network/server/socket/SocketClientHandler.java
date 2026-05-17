package it.polimi.ingsw.am22.network.server.socket;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.server.ClientHandler;
import it.polimi.ingsw.am22.controller.server.MatchManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Both {@link ClientHandler} (synchronized send via ObjectOutputStream) and
 * {@link Runnable} (read loop for incoming ClientRequests). On EOF/IO error
 * notifies the match manager via {@link MatchManager#handleTransportDrop}.
 */
public class SocketClientHandler implements ClientHandler, Runnable {
    private final Socket socket;
    private final MatchManager matchManager;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile String boundNickname;
    private volatile String boundMatchId;
    private volatile boolean closed;

    public SocketClientHandler(Socket socket, MatchManager matchManager) throws IOException {
        this.socket = socket;
        this.matchManager = matchManager;
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                Object incoming = inputStream.readObject();
                if (incoming instanceof ClientRequest request) {
                    matchManager.handleRequest(request, this);
                } else {
                    send(new ErrorMessage("Invalid payload received."));
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!closed) {
                matchManager.handleTransportDrop(this);
            }
        } finally {
            close();
        }
    }

    @Override
    public synchronized void send(ServerMessage message) {
        if (closed) return;
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
        if (closed) return;
        closed = true;
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    @Override public String getBoundNickname() { return boundNickname; }
    @Override public void setBoundNickname(String nickname) { this.boundNickname = nickname; }
    @Override public String getBoundMatchId() { return boundMatchId; }
    @Override public void setBoundMatchId(String matchId) { this.boundMatchId = matchId; }
}
