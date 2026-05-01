package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.network.client.ClientApp;

/**
 * Launcher storico mantenuto per compatibilità con eventuali run
 * configuration che puntano a {@code it.polimi.ingsw.am22.model.Launcher}.
 *
 * <p>Delega al vero entry point del client ({@link ClientApp}) che gestisce
 * la scelta TUI/GUI e l'intero flusso di connessione.
 */
public class Launcher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
