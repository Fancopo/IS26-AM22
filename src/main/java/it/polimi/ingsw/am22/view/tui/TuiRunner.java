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
     *
     * <p>La connessione col server vive per tutta la durata del processo:
     * un eventuale {@code leave} (lobby o partita) lascia il canale aperto,
     * azzera lo stato locale di match e riporta il giocatore alla
     * situazione iniziale (può di nuovo {@code list}, {@code create},
     * {@code join}) sulla stessa sessione.
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

        // Outer loop supports the end-game 'back' command: when the player
        // picks 'back' on the end-game menu we close the just-finished
        // session and reopen a fresh one against the same host/port —
        // mirroring the GUI's endGameAndShowMatches flow.
        boolean firstSession = true;
        while (true) {
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

            if (firstSession) {
                printHelp();
                firstSession = false;
            } else {
                System.out.println(Ansi.green(
                        "(reconnected — type 'list' to see open matches)"));
            }

            commandLoop(in, session, view);

            boolean serverDropped = view.wasDisconnectedByServer();
            boolean expectedClose = view.isExpectingDisconnect();
            boolean wantsReconnect = view.isReconnectRequested();
            // After EndGame the channel is already gone (or about to be):
            // don't try to send a disconnect notification through it.
            session.close(!serverDropped && !expectedClose);

            if (wantsReconnect) {
                continue;
            }
            System.out.println("Bye.");
            // Only signal an error exit when the drop was unexpected
            // (i.e. NOT the post-EndGame courtesy close).
            if (serverDropped && !expectedClose) {
                System.exit(1);
            }
            return;
        }
    }

    /**
     * Loop principale di lettura comandi da stdin.
     * Per ogni riga inserita dall'utente fa il parse del primo token e lo
     * mappa su un comando: pre-lobby ({@code list}/{@code create}/{@code join}),
     * in-match ({@code place}/{@code pick}/{@code bonus}/{@code players}/{@code leave}),
     * end-game ({@code back}/{@code leaderboard}). Esce solo quando la TuiView
     * imposta {@code stopRequested} (es. comando {@code quit} o disconnessione).
     */
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
                        // Un client può essere iscritto a una sola partita per volta:
                        // se ne ha già una bound (matchId non null), serve prima
                        // 'leave' / 'disconnect' — altrimenti finiremmo iscritti
                        // a due match contemporaneamente lato server.
                        requireAlreadyOutOfMatch(controller, "create");
                        requireArgs(parts, 3, "create <expectedPlayers> <nickname>");
                        int expected = Integer.parseInt(parts[1]);
                        controller.createMatch(parts[2], expected);
                    }
                    case "join" -> {
                        // join <matchId> <nickname>
                        // Stesso vincolo di 'create': un solo match alla volta.
                        requireAlreadyOutOfMatch(controller, "join");
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
                        // L'ordine in cui i cardId compaiono sulla riga di comando
                        // viene preservato dalla List e quindi spedito così com'è
                        // al server: rilevante perché Builder->Building applica lo
                        // sconto, Building->Builder no, e analogamente per gli
                        // Hunter con/senza simbolo.
                        //
                        // Prima di spedire, la TuiView stampa un echo della
                        // sequenza con detailType colorato (BUILDER/BUILDING,
                        // HUNTER*/HUNTER, ...): così il giocatore *vede subito*
                        // l'ordine che sta inviando e può cogliere un eventuale
                        // refuso ("ho scritto 98 9 invece di 9 98") confrontando
                        // la riga di echo con l'intenzione strategica, senza
                        // dover aspettare la risposta del server per scoprire
                        // un food sbagliato.
                        List<String> ids = new ArrayList<>(parts.length - 1);
                        for (int i = 1; i < parts.length; i++) ids.add(parts[i]);
                        view.echoPickOrder(ids);
                        controller.pickCards(ids);
                    }
                    case "bonus" -> {
                        requireArgs(parts, 2, "bonus <cardId>");
                        controller.pickBonusCard(parts[1]);
                    }
                    case "leave" -> {
                        // 'leave' funziona sia pre-game (uscita lobby) sia
                        // mid-game (abort match). In entrambi i casi il
                        // server NON chiude il canale: il client resta
                        // connesso, azzera lo stato locale di match e il
                        // giocatore può subito riemettere list/create/join
                        // come dalla situazione iniziale.
                        if (controller.getMatchId() == null) {
                            System.out.println("You are not in any lobby or match.");
                            break;
                        }
                        boolean midGame = session.isGameStarted();
                        if (midGame) {
                            controller.disconnect();
                        } else {
                            controller.removePlayerFromLobby();
                        }
                        session.clearLocalMatchState();
                        System.out.println(Ansi.yellow(midGame
                                ? "(match aborted — back to matches selection)"
                                : "(left lobby — back to matches selection)"));
                        System.out.println(Ansi.dim("(type 'list' to see open matches)"));
                    }
                    case "quit", "exit" -> view.requestStop();

                    // ---- End-game menu (after EndGameMessage, before exit) ----
                    case "back", "back-to-matches", "backtomatches" -> {
                        // Equivalent to GUI's "Back to matches" button:
                        // tear down the just-finished session and start a
                        // fresh connection to the same host/port.
                        if (!view.isInEndGame()) {
                            System.out.println(
                                    "'back' is for after the game ends. "
                                    + "Use 'leave' to exit a lobby/match in progress.");
                            break;
                        }
                        view.requestReconnect();
                    }
                    case "leaderboard", "lb" -> {
                        // Equivalent to GUI's "Show full leaderboard" button.
                        if (!view.isInEndGame()) {
                            System.out.println(
                                    "Leaderboard is shown only at the end of a match.");
                            break;
                        }
                        view.replayHistoricalLeaderboard();
                    }

                    default -> System.out.println("Unknown command. Type 'help'.");
                }
            } catch (RuntimeException e) {
                System.err.println("Command error: " + e.getMessage());
            }
        }
    }

    /**
     * Stampa la lista di comandi disponibili con una breve descrizione.
     * Invocato all'avvio (subito dopo la connessione) e quando l'utente
     * digita {@code help} o {@code ?}.
     */
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
        System.out.println("    pick <id1> [id2 ...]            ");
        System.out.println("    bonus <cardId>                  select bonus card");
        System.out.println("    leave                           leave the current lobby; mid-game aborts the match — returns to matches list");
        System.out.println("    quit                            quit the client");
        System.out.println();
        System.out.println("  After the game ends:");
        System.out.println("    back                            return to matches selection (reopens a connection)");
        System.out.println("    leaderboard | lb                re-display the historical leaderboard");
        System.out.println("    exit                            quit the client");
        System.out.println();
    }

    /**
     * Ristampa l'ultimo {@code GameStateDTO} o {@code LobbyStateDTO} ricevuto
     * dal server, senza fare alcuna richiesta di rete. Utile quando il
     * terminale e' "pieno" di echo dei comandi e si vuole ricaricare la vista.
     * Comando {@code state}.
     */
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

    /**
     * Chiede all'utente il tipo di trasporto da usare (Socket TCP o RMI).
     * Accetta varianti abbreviate ({@code s}/{@code tcp} per socket,
     * {@code r} per RMI); ripete la domanda finche' non viene scelto un valore valido.
     */
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

    /**
     * Helper di lettura input testuale: stampa il prompt e ritorna la prima
     * riga non vuota. Se l'utente preme Invio senza scrivere nulla e
     * {@code defaultValue} non e' null, restituisce il default.
     */
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

    /**
     * Variante di {@link #ask} che converte l'input in intero, ripetendo
     * la domanda se il valore non e' un numero valido. Usato per la porta.
     */
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

    /**
     * Verifica che il comando abbia almeno {@code expected} token (incluso
     * il nome del comando stesso). Se mancano argomenti lancia
     * {@link IllegalArgumentException} con il messaggio "usage: ...",
     * intercettata dal {@link #commandLoop} e stampata come errore.
     */
    private static void requireArgs(String[] parts, int expected, String usage) {
        if (parts.length < expected) {
            throw new IllegalArgumentException("usage: " + usage);
        }
    }

    /**
     * Rifiuta i comandi {@code create} / {@code join} se il client risulta
     * già iscritto a una partita. Senza questa guardia il giocatore poteva
     * digitare {@code join} due volte e finire registrato lato server in
     * due lobby contemporaneamente, comparendo come membro di entrambe.
     */
    private static void requireAlreadyOutOfMatch(ClientController controller, String command) {
        String currentMatch = controller.getMatchId();
        if (currentMatch != null && !currentMatch.isBlank()) {
            throw new IllegalStateException("you are already in match '" + currentMatch
                    + "' — use 'leave' (pre-game) or 'disconnect' before '" + command + "'");
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
