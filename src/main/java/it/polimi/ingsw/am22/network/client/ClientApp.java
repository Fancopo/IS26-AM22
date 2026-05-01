package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.view.gui.GuiApp;
import it.polimi.ingsw.am22.view.tui.TuiRunner;

import javafx.application.Application;

import java.util.Locale;
import java.util.Scanner;

/**
 * Punto di ingresso del client MESOS.
 * Main del client.
 * Legge --tui/--gui dagli args (o chiede interattivamente)
 * e lancia TuiRunner.run() o Application.launch(GuiApp.class).
 */
public final class ClientApp {

    private ClientApp() {
    }

    public static void main(String[] args) {
        Mode mode = resolveMode(args);
        switch (mode) {
            case TUI -> TuiRunner.run();
            case GUI -> Application.launch(GuiApp.class, args);
        }
    }

    /** Modalità dell'interfaccia utente richiesta. */
    private enum Mode { TUI, GUI }

    /**
     * Risolve la modalità UI: prima guarda gli argomenti, poi chiede all'utente.
     */
    private static Mode resolveMode(String[] args) {
        for (String a : args) {
            String s = a.toLowerCase(Locale.ROOT);
            if (s.equals("--tui") || s.equals("-t") || s.equals("tui")) return Mode.TUI;
            if (s.equals("--gui") || s.equals("-g") || s.equals("gui")) return Mode.GUI;
        }
        // Fallback: prompt interattivo (utile quando si lancia senza argomenti).
        System.out.println("MESOS client — choose interface:");
        System.out.println("  1) TUI (text)");
        System.out.println("  2) GUI (JavaFX)");
        System.out.print("Selection [1/2] (default 2): ");
        try (Scanner in = new Scanner(System.in)) {
            if (in.hasNextLine()) {
                String line = in.nextLine().trim();
                if (line.equals("1") || line.equalsIgnoreCase("tui") || line.equalsIgnoreCase("t")) {
                    return Mode.TUI;
                }
            }
        }
        return Mode.GUI;
    }
}
