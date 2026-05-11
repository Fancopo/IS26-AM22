package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.server.rmi.RmiGameServer;
import it.polimi.ingsw.am22.network.server.socket.SocketGameServer;

/**
 * Main del server.
 * Crea un singolo {@link NetworkGameService} multipartita,
 * avvia {@link SocketGameServer} (12345) e {@link RmiGameServer#publish}
 * (registry 1099, binding {@code MESOS_SERVER}).
 */
public final class NetworkServerLauncher {

    private NetworkServerLauncher() {
    }
    /**
     * Punto di ingresso del server. Crea un singolo {@link NetworkGameService}
     * (condiviso da tutti i trasporti), avvia il listener socket sulla porta
     * 12345 e pubblica lo stub RMI sul registry alla porta 1099 con binding
     * {@code MESOS_SERVER}. Eventuali eccezioni di apertura porta/registry
     * vengono propagate al chiamante.
     */
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
