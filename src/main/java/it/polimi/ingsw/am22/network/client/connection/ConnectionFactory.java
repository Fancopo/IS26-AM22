package it.polimi.ingsw.am22.network.client.connection;

import it.polimi.ingsw.am22.network.client.connection.rmi.RmiServerConnection;
import it.polimi.ingsw.am22.network.client.connection.socket.SocketServerConnection;

import java.io.IOException;
import java.rmi.NotBoundException;

/**
 * Builds the right {@link ObservableServerConnection} for the transport
 * chosen by the user, so the rest of the client only works against the
 * interface.
 */
public final class ConnectionFactory {

    public static final String DEFAULT_RMI_BINDING = "MESOS_SERVER";
    public static final int DEFAULT_SOCKET_PORT = 12345;
    public static final int DEFAULT_RMI_PORT = 1099;

    public enum Transport { SOCKET, RMI }

    private ConnectionFactory() {}

    public static ObservableServerConnection open(Transport transport, String host, int port)
            throws IOException, NotBoundException {
        return switch (transport) {
            case SOCKET -> new SocketServerConnection(host, port);
            case RMI    -> new RmiServerConnection(host, port, DEFAULT_RMI_BINDING);
        };
    }
}
