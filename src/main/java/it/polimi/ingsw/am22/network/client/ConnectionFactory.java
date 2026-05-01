package it.polimi.ingsw.am22.network.client;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Fabbrica centralizzata per costruire una {@link ObservableServerConnection}
 * partendo dalla scelta di trasporto fatta dall'utente.
 *
 * Passaggi chiave:
 *     l'utente sceglie Socket o RMI nella schermata di avvio;
 *     il launcher (TUI o GUI) chiama {@link #open(Transport, String, int)};
 *     la factory restituisce l'implementazione concreta già pronta a
 *         inviare richieste e ricevere {@code ServerMessage}.
 *
 * In questo modo il resto del client lavora solo contro l'interfaccia
 * {@link ObservableServerConnection}, senza dipendere dal trasporto scelto.
 */
public final class ConnectionFactory {

    /** Nome del binding RMI usato dal server (vedi {@code NetworkServerLauncher}). */
    public static final String DEFAULT_RMI_BINDING = "MESOS_SERVER";

    /** Porta di default del server socket. */
    public static final int DEFAULT_SOCKET_PORT = 12345;

    /** Porta di default del registry RMI. */
    public static final int DEFAULT_RMI_PORT = 1099;

    /** Trasporti supportati dal client. */
    public enum Transport {
        SOCKET,
        RMI
    }

    private ConnectionFactory() {
    }

    /**
     * Apre la connessione verso il server usando il trasporto indicato.
     *
     * @param transport tecnologia di rete scelta (Socket o RMI)
     * @param host      indirizzo del server
     * @param port      porta (TCP per socket, registry per RMI)
     * @return connessione osservabile già pronta all'uso
     * @throws IOException        se l'apertura del socket fallisce
     * @throws RemoteException    se il registry RMI non è raggiungibile
     * @throws NotBoundException  se il binding RMI non è presente
     */
    public static ObservableServerConnection open(Transport transport, String host, int port)
            throws IOException, NotBoundException {
        return switch (transport) {
            case SOCKET -> new SocketServerConnection(host, port);
            case RMI    -> new RmiServerConnection(host, port, DEFAULT_RMI_BINDING);
        };
    }
}
