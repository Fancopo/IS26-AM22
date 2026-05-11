package it.polimi.ingsw.am22.view.tui;

/**
 * Costanti e helper per i codici ANSI usati dalla TUI.
 *
 * <p>Funzionano sui terminali moderni (Windows Terminal, PowerShell,
 * IDE terminal, bash, zsh). Sul vecchio cmd.exe i codici vengono stampati
 * raw — accettabile per la consegna.
 */
public final class Ansi {

    public static final String RESET   = "[0m";
    public static final String BOLD    = "[1m";
    public static final String DIM     = "[2m";
    public static final String RED     = "[31m";
    public static final String GREEN   = "[32m";
    public static final String YELLOW  = "[33m";
    public static final String BLUE    = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String CYAN    = "[36m";

    /** Sequenza per pulire schermo + cursore in alto a sinistra. */
    public static final String CLEAR_SCREEN = "[2J[H";

    /** Bell character: il terminale fa un beep o flash della finestra. */
    public static final String BELL = "";

    private Ansi() {}

    /** Avvolge {@code s} con i codici ANSI per stamparla in rosso. */
    public static String red(String s)     { return RED     + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in verde. */
    public static String green(String s)   { return GREEN   + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in giallo. */
    public static String yellow(String s)  { return YELLOW  + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in blu. */
    public static String blue(String s)    { return BLUE    + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in magenta. */
    public static String magenta(String s) { return MAGENTA + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in ciano. */
    public static String cyan(String s)    { return CYAN    + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla in grassetto. */
    public static String bold(String s)    { return BOLD    + s + RESET; }
    /** Avvolge {@code s} con i codici ANSI per stamparla attenuata (dim). */
    public static String dim(String s)     { return DIM     + s + RESET; }
}
