package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.server.rmi.RmiGameServer;
import it.polimi.ingsw.am22.network.server.socket.SocketGameServer;

/**
 * Server entry point. Creates a single multi-match {@link NetworkGameService}
 * shared by all transports, starts the socket listener on 12345 and publishes
 * the RMI stub on the registry at 1099 under the binding {@code MESOS_SERVER}.
 */
public final class NetworkServerLauncher {

    private NetworkServerLauncher() {}

    public static void main(String[] args) throws Exception {
        int socketPort = 12345;
        int rmiPort = 1099;
        String bindingName = "MESOS_SERVER";

        NetworkGameService gameService = new NetworkGameService();

        SocketGameServer socketServer = new SocketGameServer(socketPort, gameService);
        socketServer.start();

        RmiGameServer.publish(rmiPort, bindingName, gameService);

        System.out.println("MESOS server online (multipartita).");
        System.out.println("Socket port: " + socketPort);
        System.out.println("RMI port: " + rmiPort + " | binding: " + bindingName);
    }
}
