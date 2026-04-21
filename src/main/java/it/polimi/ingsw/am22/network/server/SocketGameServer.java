package it.polimi.ingsw.am22.network.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server TCP che accetta connessioni dai client via socket.
 *
 * Avvia un thread daemon dedicato all'accept loop: per ogni connessione
 * accettata crea un {@link SocketClientHandler} e lo esegue su un
 * {@link ExecutorService} cached così che più client possano essere
 * serviti in parallelo.
 */
public class SocketGameServer implements AutoCloseable {
    private final int port;
    private final NetworkGameService gameService;
    private final ExecutorService clientExecutor;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running;

    /**
     * Crea il server (non avvia la listening: serve {@link #start()}).
     *
     * @param port        porta TCP su cui ascoltare
     * @param gameService servizio a cui inoltrare le richieste dei client
     */
    public SocketGameServer(int port, NetworkGameService gameService) {
        this.port = port;
        this.gameService = gameService;
        this.clientExecutor = Executors.newCachedThreadPool();
        this.serverSocket = null;
        this.acceptThread = null;
        this.running = false;
    }

    /**
     * Apre il {@link ServerSocket} e avvia l'accept thread in background.
     * Idempotente: se già avviato non fa nulla.
     *
     * @throws IOException se non è possibile aprire il socket server
     */
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

    /**
     * Loop di accettazione eseguito nell'accept thread. Per ogni connessione
     * crea un handler e lo sottomette al pool di esecuzione.
     */
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

    /** Ferma il server: chiude il server socket e spegne il pool dei worker. */
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
