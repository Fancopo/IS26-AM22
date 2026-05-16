package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory;
import it.polimi.ingsw.am22.controller.server.MatchManager;
import it.polimi.ingsw.am22.network.server.rmi.RmiServer;
import it.polimi.ingsw.am22.network.server.socket.SocketServer;

/**
 * Server entry point. Creates a single multi-match {@link MatchManager}
 * shared by all transports, starts the socket listener on
 * {@link ConnectionFactory#DEFAULT_SOCKET_PORT} and publishes the RMI stub on
 * the registry at {@link ConnectionFactory#DEFAULT_RMI_PORT} under the
 * binding {@link ConnectionFactory#DEFAULT_RMI_BINDING}.
 */
public final class ServerMain {

    private ServerMain() {}

    public static void main(String[] args) throws Exception {
        int socketPort = ConnectionFactory.DEFAULT_SOCKET_PORT;
        int rmiPort = ConnectionFactory.DEFAULT_RMI_PORT;
        String bindingName = ConnectionFactory.DEFAULT_RMI_BINDING;

        MatchManager gameService = new MatchManager();

        SocketServer socketServer = new SocketServer(socketPort, gameService);
        socketServer.start();

        RmiServer.Handle rmiHandle = RmiServer.publish(rmiPort, bindingName, gameService);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Flush running matches to disk before tearing down the transports
            // so an orderly stop is just as recoverable as a crash.
            gameService.shutdown();
            rmiHandle.shutdown();
            socketServer.close();
        }, "server-shutdown"));

        System.out.println("MESOS server online (multipartita).");
        System.out.println("Socket port: " + socketPort);
        System.out.println("RMI port: " + rmiPort + " | binding: " + bindingName);
    }
}
