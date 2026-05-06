package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.network.client.ClientApp;

/**
 * Legacy launcher kept for compatibility with run
 * configurations pointing to {@code it.polimi.ingsw.am22.model.Launcher}.
 *
 * <p>Delegates to the real client entry point ({@link ClientApp}) which handles
 * the TUI/GUI choice and the full connection flow.
 */
public class Launcher {
    public static void main(String[] args) {
        ClientApp.main(args);
    }
}
