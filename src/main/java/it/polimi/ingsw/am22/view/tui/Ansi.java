package it.polimi.ingsw.am22.view.tui;

/**
 * ANSI escape sequences used by the TUI. Work on modern terminals (Windows
 * Terminal, PowerShell, IDE terminals, bash, zsh). Legacy cmd.exe prints them
 * raw — acceptable for the project deliverable.
 */
public final class Ansi {

    public static final String RESET   = "[0m";
    public static final String BOLD    = "[1m";
    public static final String DIM     = "[2m";
    public static final String RED     = "[31m";
    public static final String GREEN   = "[32m";
    public static final String YELLOW  = "[33m";
    public static final String BLUE    = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String CYAN    = "[36m";

    public static final String CLEAR_SCREEN = "[2J[H";

    /** Bell character: the terminal beeps or flashes its window. */
    public static final String BELL = "";

    private Ansi() {}

    public static String red(String s)     { return RED     + s + RESET; }
    public static String green(String s)   { return GREEN   + s + RESET; }
    public static String yellow(String s)  { return YELLOW  + s + RESET; }
    public static String blue(String s)    { return BLUE    + s + RESET; }
    public static String magenta(String s) { return MAGENTA + s + RESET; }
    public static String cyan(String s)    { return CYAN    + s + RESET; }
    public static String bold(String s)    { return BOLD    + s + RESET; }
    public static String dim(String s)     { return DIM     + s + RESET; }
}
