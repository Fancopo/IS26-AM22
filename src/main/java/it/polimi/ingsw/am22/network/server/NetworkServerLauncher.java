package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;

/**
 * Entry point del server di rete.
 *
 * Istanzia {@link GameController} e {@link NetworkGameService}, avvia il
 * {@link SocketGameServer} sulla porta 12345 e pubblica un {@link RmiGameServer}
 * sul registry RMI (porta 1099, binding {@code MESOS_SERVER}).
 * Entrambi i trasporti condividono lo stesso {@link NetworkGameService},
 * così la logica di gioco è unificata a prescindere dal protocollo usato dal client.
 */
public final class NetworkServerLauncher {

    private NetworkServerLauncher() {
    }

    /**
     * Metodo principale: avvia i due server (socket + RMI).
     *
     * @param args non utilizzati
     * @throws Exception se l'avvio di uno dei due server fallisce
     */
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
