package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.view.gui.GuiApp;
import it.polimi.ingsw.am22.view.tui.TuiRunner;

import javafx.application.Application;

import java.util.Locale;
import java.util.Scanner;

/**
 * Client entry point. Reads --tui/--gui from args (or asks interactively) and
 * launches the chosen runner.
 */
public final class ClientMain {

    private ClientMain() {}

    public static void main(String[] args) {
        Mode mode = resolveMode(args);
        switch (mode) {
            case TUI -> TuiRunner.run();
            case GUI -> Application.launch(GuiApp.class, args);
        }
    }

    private enum Mode { TUI, GUI }

    private static Mode resolveMode(String[] args) {
        //if someone wants to specify gui/tui at the start from terminal jar
        for (String a : args) {
            String s = a.toLowerCase(Locale.ROOT);
            if (s.equals("--tui") || s.equals("-t") || s.equals("tui")) return Mode.TUI;
            if (s.equals("--gui") || s.equals("-g") || s.equals("gui")) return Mode.GUI;
        }
        //at running from intelliJ
        System.out.println("MESOS client — choose interface:");
        System.out.println("  1) TUI (text)");
        System.out.println("  2) GUI (JavaFX)");
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print("Selection [tui/gui] (default gui): ");
            if (!in.hasNextLine()) return Mode.GUI;
            String line = in.nextLine().trim().toLowerCase(Locale.ROOT);
            if (line.isEmpty()) return Mode.GUI;
            if (line.equals("1") || line.equals("tui") || line.equals("t")) return Mode.TUI;
            if (line.equals("2") || line.equals("gui") || line.equals("g")) return Mode.GUI;
            System.out.println("Input non valido: scrivi 'tui' o 'gui' (oppure invio per gui).");
        }
    }
}
