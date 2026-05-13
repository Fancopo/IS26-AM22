package it.polimi.ingsw.am22.view.tui;

/**
 * ANSI escape sequences used by the TUI. Work on modern terminals (Windows
 * Terminal, PowerShell, IDE terminals, bash, zsh). Legacy cmd.exe prints them
 * raw — acceptable for the project deliverable.
 *
 * <p>The escape character is written as {@code } on purpose: a raw 0x1B
 * byte in the source file is invisible and trivially stripped by editors or
 * by anyone reading+rewriting the file.
 */
public final class Ansi {

    private static final String ESC = "";

    public static final String RESET   = ESC + "[0m";
    public static final String BOLD    = ESC + "[1m";
    public static final String DIM     = ESC + "[2m";
    public static final String RED     = ESC + "[31m";
    public static final String GREEN   = ESC + "[32m";
    public static final String YELLOW  = ESC + "[33m";
    public static final String BLUE    = ESC + "[34m";
    public static final String MAGENTA = ESC + "[35m";
    public static final String CYAN    = ESC + "[36m";

    public static final String CLEAR_SCREEN = ESC + "[2J" + ESC + "[H";

    /** Bell character: the terminal beeps or flashes its window. */
    public static final String BELL = "";

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
