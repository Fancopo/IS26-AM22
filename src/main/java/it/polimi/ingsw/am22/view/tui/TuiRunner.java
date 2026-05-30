package it.polimi.ingsw.am22.view.tui;

import it.polimi.ingsw.am22.controller.client.VirtualServer;
import it.polimi.ingsw.am22.controller.client.ClientSession;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.connection.ConnectionFactory.Transport;
import it.polimi.ingsw.am22.network.client.connection.ServerConnection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * TUI entry point. Asks transport/host/port, opens the connection, sets up
 * a {@link ClientSession} backed by a {@link TuiView}, and runs the command
 * loop. {@code leave}/{@code disconnect} keep the channel open and reset
 * local match state — the player can list/create/join again on the same session.
 */
public final class TuiRunner {

    private TuiRunner() {}

    public static void run() {
        Scanner in = new Scanner(System.in);

        printBanner();
        Transport transport = askTransport(in);
        String host = ask(in, "Server host [127.0.0.1]: ", "127.0.0.1");
        int defaultPort = transport == Transport.SOCKET
                ? ConnectionFactory.DEFAULT_SOCKET_PORT
                : ConnectionFactory.DEFAULT_RMI_PORT;
        int port = askInt(in, "Port [" + defaultPort + "]: ", defaultPort);

        // Outer loop supports the end-game 'back' command and server-crash
        // recovery: in both cases the just-finished session is closed and a
        // fresh one is opened against the same host/port.
        boolean firstSession = true;
        // A live, already-resumed session produced by the recovery flow;
        // when set, the loop runs it directly instead of opening a new one.
        TuiView resumedView = null;

        while (true) {
            ClientSession session;
            TuiView view;

            if (resumedView != null) {
                view = resumedView;
                session = resumedView.getSession();
                resumedView = null;
            } else {
                ServerConnection connection;
                try {
                    connection = ConnectionFactory.open(transport, host, port);
                } catch (Exception e) {
                    System.err.println("Unable to connect: "
                            + ConnectionFactory.describeConnectionError(e));
                    return;
                }
                session = new ClientSession(connection);
                view = new TuiView(session);
                session.setHandler(view);
                if (firstSession) {
                    printHelp();
                    firstSession = false;
                } else {
                    System.out.println(Ansi.green(
                            "(reconnected — type 'list' to see open matches)"));
                }
            }

            commandLoop(in, session, view);

            boolean serverDropped = view.wasDisconnectedByServer();
            boolean expectedClose = view.isExpectingDisconnect();
            boolean wantsReconnect = view.isReconnectRequested();
            // Captured before close(): a server crash mid-match leaves a
            // resumable match on the server, keyed by this matchId.
            boolean wasMidGame = session.isGameStarted();
            String recoverableMatchId = session.getVirtualServer().getMatchId();
            // After EndGame the channel is already gone (or about to be):
            // don't try to send a disconnect notification through it.
            session.close(!serverDropped && !expectedClose);

            if (wantsReconnect) {
                continue;
            }

            // Server crashed while a match was running: notify the player that
            // an uncomplete match exists and offer to resume it.
            if (serverDropped && !expectedClose && wasMidGame && recoverableMatchId != null) {
                resumedView = recoverMatch(in, transport, host, port, recoverableMatchId);
                // Whether the match was resumed (resumedView != null) or the
                // player gave up (null, → fresh session to look for another
                // match), the loop just iterates again.
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
     * Server-crash recovery flow. Notifies the player that an uncomplete match
     * exists, then keeps asking for the nickname used in that match until
     * either the server accepts the reconnection or the player gives up. A
     * wrong nickname is not fatal: the player is simply asked again, and can
     * type {@code back} to abandon the recovery and look for another match.
     *
     * @return the live {@link TuiView} of the resumed session, or null if the
     *         player declined / gave up the recovery
     */
    private static TuiView recoverMatch(Scanner in, Transport transport,
                                        String host, int port, String matchId) {
        System.out.println();
        System.out.println(Ansi.yellow(Ansi.BOLD
                + "[!] Found an uncomplete match (" + matchId + ")."));
        System.out.println(Ansi.yellow(
                "    The server went down while your match was still in progress."));
        if (!askYesNo(in, "Do you want to continue the previous match? [yes/no]: ")) {
            // Declining is the TUI's "Leave this match": tell the server to
            // delete the suspended match and notify any player that already
            // reconnected, then keep this connection so the player can
            // list/create/join right away (mirrors the GUI's leave flow).
            return leaveSuspendedMatch(transport, host, port, matchId);
        }

        while (true) {
            String nickname = ask(in, "Enter the nickname you used in the previous match: ", null);

            ServerConnection connection;
            try {
                connection = ConnectionFactory.open(transport, host, port);
            } catch (Exception e) {
                System.err.println("Unable to connect: "
                        + ConnectionFactory.describeConnectionError(e));
                if (askYesNo(in, "Server still unreachable. Try again? [yes/no]: ")) {
                    continue;
                }
                return null;
            }

            ClientSession session = new ClientSession(connection);
            TuiView view = new TuiView(session);
            session.setHandler(view);
            view.armReconnect();
            System.out.println(Ansi.green(
                    "(reconnecting to match " + matchId + " as '" + nickname + "'…)"));
            session.getVirtualServer().reconnect(matchId, nickname);

            if (waitReconnectOutcome(view)) {
                return view;
            }

            session.close(false);
            // The match no longer exists (a player left it or it timed out):
            // there is nothing left to resume, so stop the recovery loop.
            if (view.isRecoveredMatchGone()) {
                return null;
            }
            // Reconnection refused (the [ERROR] line is printed by the view):
            // ask the player whether to retry by typing the nickname again.
            if (!askYesNo(in, "Reconnection failed. "
                    + "Do you want to try again by typing your nickname? [yes/no]: ")) {
                return null;
            }
        }
    }

    /**
     * Sends the "abandon suspended match" request on a fresh connection and
     * keeps it open so the returned view runs straight into the command loop —
     * the player can list/create/join again without reconnecting. Returns null
     * if the server is unreachable, in which case the caller opens a brand-new
     * session.
     */
    private static TuiView leaveSuspendedMatch(Transport transport, String host,
                                               int port, String matchId) {
        ServerConnection connection;
        try {
            connection = ConnectionFactory.open(transport, host, port);
        } catch (Exception e) {
            System.err.println("Unable to reach the server to leave the match: "
                    + ConnectionFactory.describeConnectionError(e));
            return null;
        }
        ClientSession session = new ClientSession(connection);
        TuiView view = new TuiView(session);
        session.setHandler(view);
        try {
            // Do NOT close the connection here: on RMI the request is delivered
            // asynchronously by the outbound worker; the kept-open session runs
            // into the command loop where it will eventually be closed.
            session.getVirtualServer().abandonRecoveredMatch(matchId);
        } catch (RuntimeException ignored) {
        }
        System.out.println(Ansi.yellow(
                "(left the suspended match " + matchId + " — back to matches selection)"));
        System.out.println(Ansi.dim("(type 'list' to see open matches)"));
        return view;
    }

    /**
     * Blocks until the server accepts or refuses a reconnection, or a short
     * timeout elapses.
     *
     * @return true if the match was resumed, false on refusal / timeout
     */
    private static boolean waitReconnectOutcome(TuiView view) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (view.isReconnectAccepted()) return true;
            if (view.isReconnectRejected()) return false;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        System.out.println(Ansi.red("[ERROR] No response from the server."));
        return false;
    }

    private static boolean askYesNo(Scanner in, String prompt) {
        while (true) {
            String v = ask(in, prompt, "no").toLowerCase();
            if (v.equals("yes") || v.equals("y")) return true;
            if (v.equals("no") || v.equals("n")) return false;
            System.out.println("Please type 'yes' or 'no'.");
        }
    }

    private static void commandLoop(Scanner in, ClientSession session, TuiView view) {
        VirtualServer virtualServer = session.getVirtualServer();
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
                    case "list" -> virtualServer.listMatches();
                    case "create" -> {
                        // create <expectedPlayers> <nickname>
                        // Un client può essere iscritto a una sola partita per volta:
                        // se ne ha già una bound (matchId non null), serve prima
                        // 'leave' / 'disconnect' — altrimenti finiremmo iscritti
                        // a due match contemporaneamente lato server.
                        requireAlreadyOutOfMatch(virtualServer, "create");
                        requireArgs(parts, 3, "create <expectedPlayers> <nickname>");
                        int expected = Integer.parseInt(parts[1]);
                        virtualServer.createMatch(parts[2], expected);
                    }
                    case "join" -> {
                        // join <matchId> <nickname>
                        // Stesso vincolo di 'create': un solo match alla volta.
                        requireAlreadyOutOfMatch(virtualServer, "join");
                        requireArgs(parts, 3, "join <matchId> <nickname>");
                        virtualServer.addPlayerToLobby(parts[1], parts[2]);
                    }

                    // ---- Comandi che richiedono di essere già in una partita ----
                    case "players" -> {
                        requireArgs(parts, 2, "players <N>");
                        virtualServer.setExpectedPlayers(Integer.parseInt(parts[1]));
                    }
                    case "place" -> {
                        requireArgs(parts, 2, "place <letter>");
                        if (parts[1].length() != 1) {
                            throw new IllegalArgumentException("offer letter must be a single character");
                        }
                        virtualServer.placeTotem(parts[1].charAt(0));
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
                        ids.addAll(Arrays.asList(parts).subList(1, parts.length));
                        view.echoPickOrder(ids);
                        virtualServer.pickCards(ids);
                    }
                    case "bonus" -> {
                        requireArgs(parts, 2, "bonus <cardId>");
                        virtualServer.pickBonusCard(parts[1]);
                    }
                    case "check" -> {
                        // Local lookup against the cached game state — no
                        // network round-trip. Works for any visible card:
                        // board (upper/lower row) plus every player's tribe
                        // and buildings.
                        requireArgs(parts, 2, "check <cardId>");
                        view.renderCardCheck(parts[1]);
                    }
                    case "leave" -> {
                        // 'leave' funziona sia pre-game (uscita lobby) sia
                        // mid-game (abort match). In entrambi i casi il
                        // server NON chiude il canale: il client resta
                        // connesso, azzera lo stato locale di match e il
                        // giocatore può subito riemettere list/create/join
                        // come dalla situazione iniziale.
                        if (virtualServer.getMatchId() == null) {
                            System.out.println("You are not in any lobby or match.");
                            break;
                        }
                        boolean midGame = session.isGameStarted();
                        if (midGame) {
                            virtualServer.disconnect();
                        } else {
                            virtualServer.removePlayerFromLobby();
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
        System.out.println("    check <cardId>                  show full info of a visible card (board or any tribe/buildings)");
        System.out.println("    leave                           leave the current lobby; mid-game aborts the match — returns to matches list");
        System.out.println("    quit                            quit the client");
        System.out.println();
        System.out.println("  After the game ends:");
        System.out.println("    back                            return to matches selection (reopens a connection)");
        System.out.println("    leaderboard | lb                re-display the historical leaderboard");
        System.out.println("    exit                            quit the client");
        System.out.println();
    }

    /** Re-renders the latest game/lobby state without a network round-trip. */
    private static void printCachedState(ClientSession session) {
        if (session.getLatestGameState() != null) {
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.protocol.message.response.GameStateMessage(
                            session.getLatestGameState()));
        } else if (session.getLatestLobbyState() != null) {
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.protocol.message.response.LobbyStateMessage(
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

    /**
     * Without this guard a player could type {@code join} twice and end up
     * registered server-side in two lobbies at once.
     */
    private static void requireAlreadyOutOfMatch(VirtualServer virtualServer, String command) {
        String currentMatch = virtualServer.getMatchId();
        if (currentMatch != null && !currentMatch.isBlank()) {
            throw new IllegalStateException("you are already in match '" + currentMatch
                    + "' — use 'leave' (pre-game) or 'disconnect' before '" + command + "'");
        }
    }

    private static void printBanner() {
        System.out.println(Ansi.cyan(
                """
                          __  __ _____ ____   ___  ____ \s
                         |  \\/  | ____/ ___| / _ \\/ ___|\s
                         | |\\/| |  _| \\___ \\| | | \\___ \\\s
                         | |  | | |___ ___) | |_| |___) |
                         |_|  |_|_____|____/ \\___/|____/ \
                        """));
        System.out.println(Ansi.dim("                Mesolithic Tribes — TUI client (multipartita)\n"));
    }

    private static void printWho(ClientSession session) {
        VirtualServer virtualServer = session.getVirtualServer();
        String nick = virtualServer.getNickname();
        String matchId = virtualServer.getMatchId();
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
