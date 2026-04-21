package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Handler server-side per un singolo client connesso via socket.
 *
 * Agisce come {@link ClientChannel} (per l'invio dei messaggi verso il client)
 * e come {@link Runnable} (per il loop di lettura delle richieste in arrivo).
 * Viene eseguito nel pool di {@link SocketGameServer}: un'istanza per client.
 * In caso di errore di I/O notifica il servizio tramite
 * {@link NetworkGameService#handleTransportDrop(ClientChannel)}.
 */
public class SocketClientHandler implements ClientChannel, Runnable {
    private final Socket socket;
    private final NetworkGameService gameService;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;
    private volatile String boundNickname;
    private volatile boolean closed;

    /**
     * Costruisce l'handler e inizializza gli stream di I/O sul socket.
     *
     * @param socket      socket già connesso al client
     * @param gameService servizio a cui inoltrare le richieste
     * @throws IOException se non è possibile inizializzare gli stream
     */
    public SocketClientHandler(Socket socket, NetworkGameService gameService) throws IOException {
        this.socket = socket;
        this.gameService = gameService;
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(socket.getInputStream());
        this.boundNickname = null;
        this.closed = false;
    }

    /**
     * Loop di lettura: riceve {@link ClientRequest} dal client e le inoltra al servizio.
     * Un payload non riconosciuto produce un {@link ErrorMessage}.
     * Errori di I/O causano la notifica di transport drop al servizio.
     */
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

    /**
     * Invia un messaggio al client. È {@code synchronized} per evitare scritture
     * concorrenti sullo stream condiviso.
     *
     * @param message messaggio da inviare
     * @throws IllegalStateException se l'invio fallisce
     */
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

    /** Chiude il socket sottostante in modo idempotente. */
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
