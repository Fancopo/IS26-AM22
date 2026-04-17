package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;

public final class NetworkServerLauncher {
    private NetworkServerLauncher() {
    }

    public static void main(String[] args) throws Exception {
        int socketPort = 12345;
        int rmiPort = 1099;
        String bindingName = "MESOS_SERVER";

        GameController gameController = new GameController();
        NetworkGameService gameService = new NetworkGameService(gameController);

        SocketGameServer socketServer = new SocketGameServer(socketPort, gameService);
        socketServer.start();

        RmiGameServer.publish(rmiPort, bindingName, gameService);

        System.out.println("MESOS server online.");
        System.out.println("Socket port: " + socketPort);
        System.out.println("RMI port: " + rmiPort + " | binding: " + bindingName);
    }
}
