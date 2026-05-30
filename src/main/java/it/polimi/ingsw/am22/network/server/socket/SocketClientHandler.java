package it.polimi.ingsw.am22.network.server.socket;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.request.PingRequest;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.PingMessage;
import it.polimi.ingsw.am22.network.server.ClientHandler;
import it.polimi.ingsw.am22.controller.server.MatchManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Both {@link ClientHandler} (synchronized send via ObjectOutputStream) and
 * {@link Runnable} (read loop for incoming ClientRequests). On EOF/IO error
 * notifies the match manager via {@link MatchManager#handleTransportDrop}.
 *
 * <p>Brutally-dropped TCP connections (cable unplugged, Wi-Fi gone, NAT idle
 * timeout) are not signalled by the kernel for hours (default keepalive ~2 h).
 * To detect them quickly the handler combines two mechanisms:
 * <ul>
 *   <li>{@code setSoTimeout(READ_TIMEOUT_MS)} on the socket — {@code readObject}
 *       throws {@link SocketTimeoutException} after that long of silence; the
 *       read loop swallows it and keeps looping, so the timeout itself is not
 *       a disconnect signal but it guarantees the loop never blocks forever;</li>
 *   <li>scheduled outbound {@link PingMessage} every {@code PING_INTERVAL_MS} —
 *       a failed write surfaces as {@code IOException} and routes through the
 *       normal drop path. This is the actual liveness probe.</li>
 * </ul>
 */
public class SocketClientHandler implements ClientHandler, Runnable {
    /** Drop the connection if no traffic for this long: catches brutal drops in seconds. */
    private static final int READ_TIMEOUT_MS = 3000;
    /** Outbound ping cadence; 3× lower than the read timeout to avoid false positives. */
    private static final long PING_INTERVAL_MS = 1000;

    private final Socket socket;
    private final MatchManager matchManager;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private final ScheduledExecutorService livenessProbe;
    private volatile String boundNickname;
    private volatile String boundMatchId;
    private volatile boolean closed;

    public SocketClientHandler(Socket socket, MatchManager matchManager) throws IOException {
        this.socket = socket;
        this.matchManager = matchManager;
        socket.setSoTimeout(READ_TIMEOUT_MS);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.livenessProbe = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "socket-server-ping");
            t.setDaemon(true);
            return t;
        });
        livenessProbe.scheduleWithFixedDelay(this::pingProbe,
                PING_INTERVAL_MS, PING_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void pingProbe() {
        if (closed) return;
        try {
            send(new PingMessage());
        } catch (Exception ignored) {
            // send() has already escalated the drop to MatchManager via close().
        }
    }

    @Override
    public void run() {
        try {
            while (!closed) {
                Object incoming;
                try {
                    incoming = inputStream.readObject();
                } catch (SocketTimeoutException timeout) {
                    // No traffic in READ_TIMEOUT_MS — keep looping. The outbound
                    // ping is the actual liveness signal; the timeout is here
                    // only to keep the read loop responsive to close().
                    continue;
                }
                if (incoming instanceof PingRequest) {
                    // Transport-only: don't bother the controller.
                    continue;
                }
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
        livenessProbe.shutdownNow();
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
