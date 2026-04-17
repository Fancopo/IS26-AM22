package it.polimi.ingsw.am22.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketGameServer implements AutoCloseable {
    private final int port;
    private final NetworkGameService gameService;
    private final ExecutorService clientExecutor;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    public SocketGameServer(int port, NetworkGameService gameService) {
        this.port = port;
        this.gameService = gameService;
        this.clientExecutor = Executors.newCachedThreadPool();
        this.serverSocket = null;
        this.acceptThread = null;
        this.running = false;
    }

    public void start() throws IOException {
        if (running) {
            return;
        }
        this.serverSocket = new ServerSocket(port);
        this.running = true;
        this.acceptThread = new Thread(this::acceptLoop, "socket-game-server-accept");
        this.acceptThread.setDaemon(true);
        this.acceptThread.start();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                SocketClientHandler handler = new SocketClientHandler(clientSocket, gameService);
                clientExecutor.submit(handler);
            } catch (IOException e) {
                if (running) {
                    throw new IllegalStateException("Unable to accept a socket client connection.", e);
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        clientExecutor.shutdownNow();
    }
}
