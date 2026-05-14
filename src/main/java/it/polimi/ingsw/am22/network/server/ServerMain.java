package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.server.transport.rmi.RmiServerEndpoint;
import it.polimi.ingsw.am22.network.server.transport.socket.SocketServerAcceptor;

/**
 * Server entry point. Creates a single multi-match {@link MatchManager}
 * shared by all transports, starts the socket listener on 12345 and publishes
 * the RMI stub on the registry at 1099 under the binding {@code MESOS_SERVER}.
 */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        int socketPort = 12345;
        int rmiPort = 1099;
        String bindingName = "MESOS_SERVER";

        MatchManager gameService = new MatchManager();

        SocketServerAcceptor socketServer = new SocketServerAcceptor(socketPort, gameService);
        socketServer.start();

        RmiServerEndpoint.publish(rmiPort, bindingName, gameService);

        System.out.println("MESOS server online (multipartita).");
        System.out.println("Socket port: " + socketPort);
        System.out.println("RMI port: " + rmiPort + " | binding: " + bindingName);
    }
}
