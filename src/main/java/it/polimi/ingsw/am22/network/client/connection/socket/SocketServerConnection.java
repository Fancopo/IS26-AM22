package it.polimi.ingsw.am22.network.client.connection.socket;

import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.request.PingRequest;
import it.polimi.ingsw.am22.network.protocol.message.response.PingMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * TCP socket {@link ServerConnection}. A daemon reader thread reads
 * incoming {@link ServerMessage}s and forwards them to the registered handler.
 * {@link #send} is synchronized to keep writes on the shared output stream atomic.
 *
 * <p>Brutal disconnect detection mirrors the server side
 * ({@code SocketClientHandler}): {@code setSoTimeout} keeps the read loop
 * responsive, and a scheduled outbound {@link PingRequest} surfaces dead
 * peers within a couple of seconds instead of the kernel default (~2 h).
 */
public class SocketServerConnection implements ServerConnection {
    /** Drop the connection if no traffic for this long: catches brutal drops in seconds. */
    private static final int READ_TIMEOUT_MS = 3000;
    /** Outbound ping cadence; 3× lower than the read timeout to avoid false positives. */
    private static final long PING_INTERVAL_MS = 1000;

    private final Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile ServerHandler updateHandler;
    private final Thread readerThread;
    private final ScheduledExecutorService livenessProbe;
    private volatile boolean closed;

    public SocketServerConnection(String host, int port) throws IOException {
        this.socket = new Socket(Objects.requireNonNull(host, "host cannot be null"), port);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.readerThread = new Thread(this::readLoop, "socket-server-connection-reader");
        this.readerThread.setDaemon(true);
        this.readerThread.start();
        this.livenessProbe = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "socket-client-ping");
            t.setDaemon(true);
            return t;
        });
        livenessProbe.scheduleWithFixedDelay(this::pingProbe,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pingProbe() {
        if (closed) return;
        try {
            send(new PingRequest());
        } catch (Exception ignored) {
            // send() already closed the connection; readLoop will surface the drop.
        }
    }

    @Override
    public void setMessageDispatcher(ServerHandler handler) {
        this.updateHandler = handler;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        livenessProbe.shutdownNow();
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
                Object incoming;
                try {
                    incoming = inputStream.readObject();
                } catch (SocketTimeoutException timeout) {
                    // No traffic in READ_TIMEOUT_MS — keep looping; the outbound
                    // ping is the actual liveness signal.
                    continue;
                }
                if (incoming instanceof PingMessage) {
                    // Transport-only: don't bother the view.
                    continue;
                }
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
