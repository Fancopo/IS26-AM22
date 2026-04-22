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
 * Entry point della modalità TUI.
 *
 * <p>Flusso eseguito in {@link #run()}:
 * <ol>
 *     <li>chiede trasporto (Socket/RMI), host, porta e nickname;</li>
 *     <li>apre la {@link ObservableServerConnection} tramite {@link ConnectionFactory};</li>
 *     <li>crea la {@link ClientSession} e collega la {@link TuiView};</li>
 *     <li>manda {@code addPlayerToLobby(nickname)} e poi entra nel loop comandi.</li>
 * </ol>
 *
 * <p>Il loop comandi legge righe da stdin e le interpreta. I messaggi del
 * server vengono stampati in modo asincrono dalla TuiView: qui non dobbiamo
 * farci niente oltre a gestire la sincronizzazione con {@code printLock}.
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

        // 1) Setup connessione.
        System.out.println("== MESOS client (TUI) ==");
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
        // Registra la view come handler: d'ora in poi i messaggi dal server
        // vengono renderizzati a schermo automaticamente.
        session.setHandler(view);

        // 2) Join lobby.
        String nickname = ask(in, "Nickname: ", null);
        try {
            session.getClientController().addPlayerToLobby(nickname);
        } catch (RuntimeException e) {
            System.err.println("Join failed: " + e.getMessage());
            session.close(false);
            return;
        }

        // 3) Loop comandi: continua finché la view non chiede lo stop
        //    (quit, game over, disconnect server...).
        printHelp();
        commandLoop(in, session, view);

        // 4) Cleanup: chiudiamo la connessione notificando il server se possibile.
        session.close(!view.isStopRequested());
        System.out.println("Bye.");
    }

    /**
     * Loop principale: legge una riga alla volta e la interpreta come comando.
     * Gli errori locali vengono stampati a video ma non interrompono il loop.
     */
    private static void commandLoop(Scanner in, ClientSession session, TuiView view) {
        ClientController controller = session.getClientController();
        while (!view.isStopRequested()) {
            if (!in.hasNextLine()) {
                // stdin chiuso: esci.
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
                    case "players"   -> {
                        // host imposta il numero di giocatori attesi
                        requireArgs(parts, 2, "players <N>");
                        controller.setExpectedPlayers(Integer.parseInt(parts[1]));
                    }
                    case "place"     -> {
                        // piazzamento totem sulla tessera offerta indicata dalla lettera
                        requireArgs(parts, 2, "place <letter>");
                        if (parts[1].length() != 1) {
                            throw new IllegalArgumentException("offer letter must be a single character");
                        }
                        controller.placeTotem(parts[1].charAt(0));
                    }
                    case "pick"      -> {
                        // selezione carte dalla board (lista di id)
                        List<String> ids = new ArrayList<>(parts.length - 1);
                        for (int i = 1; i < parts.length; i++) ids.add(parts[i]);
                        controller.pickCards(ids);
                    }
                    case "bonus"     -> {
                        requireArgs(parts, 2, "bonus <cardId>");
                        controller.pickBonusCard(parts[1]);
                    }
                    case "leave"     -> {
                        controller.removePlayerFromLobby();
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
        System.out.println("  help                     show this help");
        System.out.println("  state                    print last known game/lobby state");
        System.out.println("  players <N>              (host only) set expected players");
        System.out.println("  place <letter>           place totem on offer tile <letter>");
        System.out.println("  pick <id1> [id2 ...]     pick cards from the board");
        System.out.println("  bonus <cardId>           select bonus card");
        System.out.println("  leave                    leave the lobby / disconnect");
        System.out.println("  quit                     quit the client");
        System.out.println();
    }

    private static void printCachedState(ClientSession session) {
        if (session.getLatestGameState() != null) {
            // Simuliamo un "refresh" rieseguendo il render dell'ultimo stato noto.
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.common.message.response.GameStateMessage(
                            session.getLatestGameState()));
        } else if (session.getLatestLobbyState() != null) {
            new TuiView(session).onServerMessage(
                    new it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage(
                            session.getLatestLobbyState()));
        } else {
            System.out.println("(no state received yet)");
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
        System.out.print(prompt);
        String line = in.hasNextLine() ? in.nextLine().trim() : "";
        if (line.isEmpty() && defaultValue != null) return defaultValue;
        if (line.isEmpty()) return ask(in, prompt, null);
        return line;
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
}
