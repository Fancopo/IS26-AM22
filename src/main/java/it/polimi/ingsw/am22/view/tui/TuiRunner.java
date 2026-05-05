package it.polimi.ingsw.am22.view.tui;

import it.polimi.ingsw.am22.network.client.ClientController;
import it.polimi.ingsw.am22.network.client.ClientSession;
import it.polimi.ingsw.am22.network.client.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.ObservableServerConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Entry point della modalità TUI in versione multipartita.
 *
 * <p>Flusso eseguito in {@link #run()}:
 * <ol>
 *     <li>chiede trasporto (Socket/RMI), host e porta;</li>
 *     <li>apre la {@link ObservableServerConnection} tramite {@link ConnectionFactory};</li>
 *     <li>crea la {@link ClientSession} e collega la {@link TuiView};</li>
 *     <li>entra nel loop comandi: prima del join sono disponibili
 *         {@code list}, {@code create}, {@code join}; una volta dentro a una
 *         partita si abilitano i comandi di lobby/gioco.</li>
 * </ol>
 *
 * <p>I messaggi del server vengono stampati in modo asincrono dalla TuiView
 * (replay automatico al cambio di stato gestito da {@link ClientSession}).
 */
public final class TuiRunner {

    private TuiRunner() {
    }

    /**
     * Avvia la sessione TUI. Il metodo ritorna solo quando l'utente esce
     * o la connessione viene chiusa.
     */
    public static void run() {
        Scanner in = new Scanner(System.in);

        printBanner();
        Transport transport = askTransport(in);
        String host = ask(in, "Server host [127.0.0.1]: ", "127.0.0.1");
        int defaultPort = transport == Transport.SOCKET
                ? ConnectionFactory.DEFAULT_SOCKET_PORT
                : ConnectionFactory.DEFAULT_RMI_PORT;
        int port = askInt(in, "Port [" + defaultPort + "]: ", defaultPort);

        ObservableServerConnection connection;
        try {
            connection = ConnectionFactory.open(transport, host, port);
        } catch (Exception e) {
            System.err.println("Unable to connect: " + e.getClass().getSimpleName()
                    + (e.getMessage() == null ? "" : " - " + e.getMessage()));
            return;
        }

        ClientSession session = new ClientSession(connection);
        TuiView view = new TuiView(session);
        session.setHandler(view);

        // Loop comandi: parte in modalità "fuori da una partita". Una volta
        // dentro a una lobby si sbloccano i comandi specifici.
        printHelp();
        commandLoop(in, session, view);

        // Cleanup: notifica il server solo se non è già caduta la connessione.
        session.close(!view.isStopRequested());
        System.out.println("Bye.");
    }

    private static void commandLoop(Scanner in, ClientSession session, TuiView view) {
        ClientController controller = session.getClientController();
        while (!view.isStopRequested()) {
            if (!in.hasNextLine()) {
                break;
            }
            String raw = in.nextLine().trim();
            if (raw.isEmpty()) continue;
            String[] parts = raw.split("\\s+");
            String cmd = parts[0].toLowerCase();
            try {
                switch (cmd) {
                    case "help", "?" -> printHelp();
                    case "state"     -> printCachedState(session);
                    case "who", "me" -> printWho(session);

                    // ---- Pre-lobby (multipartita) ----
                    case "list" -> controller.listMatches();
                    case "create" -> {
                        // create <expectedPlayers> <nickname>
                        requireArgs(parts, 3, "create <expectedPlayers> <nickname>");
                        int expected = Integer.parseInt(parts[1]);
                        controller.createMatch(parts[2], expected);
                    }
                    case "join" -> {
                        // join <matchId> <nickname>
                        requireArgs(parts, 3, "join <matchId> <nickname>");
                        controller.addPlayerToLobby(parts[1], parts[2]);
                    }

                    // ---- Comandi che richiedono di essere già in una partita ----
                    case "players" -> {
                        requireArgs(parts, 2, "players <N>");
                        controller.setExpectedPlayers(Integer.parseInt(parts[1]));
                    }
                    case "place" -> {
                        requireArgs(parts, 2, "place <letter>");
                        if (parts[1].length() != 1) {
                            throw new IllegalArgumentException("offer letter must be a single character");
                        }
                        controller.placeTotem(parts[1].charAt(0));
                    }
                    case "pick" -> {
                        List<String> ids = new ArrayList<>(parts.length - 1);
                        for (int i = 1; i < parts.length; i++) ids.add(parts[i]);
                        controller.pickCards(ids);
                    }
                    case "bonus" -> {
                        requireArgs(parts, 2, "bonus <cardId>");
                        controller.pickBonusCard(parts[1]);
                    }
                    case "leave" -> {
                        controller.removePlayerFromLobby();
                        view.requestStop();
                    }
                    case "disconnect" -> {
                        controller.disconnect();
                        view.requestStop();
                    }
                    case "quit", "exit" -> view.requestStop();
                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            } catch (RuntimeException e) {
                System.err.println("Command error: " + e.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  help                              show this help");
        System.out.println("  state                             print last known game/lobby state");
        System.out.println("  who | me                          show your nickname and active player");
        System.out.println();
        System.out.println("  Pre-lobby (multipartita):");
        System.out.println("    list                            list open matches");
        System.out.println("    create <expectedPlayers> <nick> create a new match and join as host");
        System.out.println("    join <matchId> <nick>           join an existing open match");
        System.out.println();
        System.out.println("  In a match:");
        System.out.println("    players <N>                     (host only) update expected players");
        System.out.println("    place <letter>                  place totem on offer tile <letter>");
        System.out.println("    pick <id1> [id2 ...]            pick cards from the board");
        System.out.println("    bonus <cardId>                  select bonus card");
        System.out.println("    leave                           leave the current lobby (pre-game only)");
        System.out.println("    disconnect                      disconnect (pre-game = leave; mid-game = aborts match)");
        System.out.println("    quit                            quit the client");
        System.out.println();
    }

    private static void printCachedState(ClientSession session) {
        if (session.getLatestGameState() != null) {
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.common.message.response.GameStateMessage(
                            session.getLatestGameState()));
        } else if (session.getLatestLobbyState() != null) {
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage(
                            session.getLatestLobbyState()));
        } else {
            System.out.println("(no state received yet — try 'list' or 'create')");
        }
    }

    // -------------------- Utility di input --------------------

    private static Transport askTransport(Scanner in) {
        while (true) {
            String v = ask(in, "Transport [socket/rmi] (default socket): ", "socket").toLowerCase();
            switch (v) {
                case "socket", "s", "tcp" -> { return Transport.SOCKET; }
                case "rmi", "r"           -> { return Transport.RMI; }
                default -> System.out.println("Please type 'socket' or 'rmi'.");
            }
        }
    }

    private static String ask(Scanner in, String prompt, String defaultValue) {
        while (true) {
            System.out.print(prompt);
            if (!in.hasNextLine()) {
                return defaultValue != null ? defaultValue : "";
            }
            String line = in.nextLine().trim();
            if (!line.isEmpty()) return line;
            if (defaultValue != null) return defaultValue;
        }
    }

    private static int askInt(Scanner in, String prompt, int defaultValue) {
        while (true) {
            String s = ask(in, prompt, String.valueOf(defaultValue));
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
        }
    }

    private static void requireArgs(String[] parts, int expected, String usage) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("usage: " + usage);
        }
    }

    /** Banner ASCII di benvenuto, stampato una volta all'avvio. */
    private static void printBanner() {
        System.out.println(Ansi.cyan(
                "  __  __ _____ ____   ___  ____  \n" +
                " |  \\/  | ____/ ___| / _ \\/ ___| \n" +
                " | |\\/| |  _| \\___ \\| | | \\___ \\ \n" +
                " | |  | | |___ ___) | |_| |___) |\n" +
                " |_|  |_|_____|____/ \\___/|____/ "));
        System.out.println(Ansi.dim("                Mesolithic Tribes — TUI client (multipartita)\n"));
    }

    /**
     * Stampa nickname locale + matchId + active player. Comando 'who' / 'me'.
     * Utile in test multi-client per non confondere le finestre.
     */
    private static void printWho(ClientSession session) {
        ClientController controller = session.getClientController();
        String nick = controller.getNickname();
        String matchId = controller.getMatchId();
        System.out.println("You: " + (nick == null ? "(not joined)" : Ansi.bold(nick))
                + (matchId == null ? "" : "   match=" + Ansi.bold(matchId)));
        if (session.getLatestGameState() != null) {
            String active = session.getLatestGameState().activePlayer();
            String mark   = (active != null && active.equalsIgnoreCase(nick))
                    ? Ansi.green("  ← that's you!") : "";
            System.out.println("Active player: " + Ansi.bold(active) + mark);
        }
    }
}
