package it.polimi.ingsw.am22.network.client.connection.socket;

import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;

/**
 * TCP socket {@link ServerConnection}. A daemon reader thread reads
 * incoming {@link ServerMessage}s and forwards them to the registered handler.
 * {@link #send} is synchronized to keep writes on the shared output stream atomic.
 */
public class SocketServerConnection implements ServerConnection {
    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile ServerHandler updateHandler;
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
    public void setMessageDispatcher(ServerHandler handler) {
        this.updateHandler = handler;
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

    @Override
    public synchronized void send(ClientRequest request) {
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
                    ServerHandler handler = updateHandler;
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
            ServerHandler handler = updateHandler;
            if (handler != null) {
                handler.onConnectionClosed(cause);
            }
        }
    }
}
