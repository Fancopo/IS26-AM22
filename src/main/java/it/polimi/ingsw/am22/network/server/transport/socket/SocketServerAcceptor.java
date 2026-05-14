package it.polimi.ingsw.am22.network.server.transport.socket;

import it.polimi.ingsw.am22.network.server.MatchManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** ServerSocket + daemon accept thread + cached thread pool: one task per client. */
public class SocketServerAcceptor implements AutoCloseable {
    private final int port;
    private final MatchManager gameService;
    private final ExecutorService clientExecutor;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public SocketServerAcceptor(int port, MatchManager gameService) {
        this.port = port;
        this.gameService = gameService;
        this.clientExecutor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        if (running) return;
        this.serverSocket = new ServerSocket(port);
        this.running = true;
        this.acceptThread = new Thread(this::acceptLoop, "socket-game-server-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    /**
     * Per-client failures (e.g. a peer that opens the TCP socket and immediately
     * closes it without sending the ObjectOutputStream header — port scanners,
     * health checks) only skip that one connection. Only a failure of accept()
     * itself tears the server down.
     */
    private void acceptLoop() {
        while (running) {
            Socket clientSocket;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                if (running) {
                    throw new IllegalStateException("Unable to accept a socket client connection.", e);
                }
                return;
            }
            try {
                clientExecutor.submit(new SocketClientHandler(clientSocket, gameService));
            } catch (IOException e) {
                // Drive-by connection: peer disconnected before sending the ObjectStream header.
                try {
                    clientSocket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        clientExecutor.shutdownNow();
    }
}
