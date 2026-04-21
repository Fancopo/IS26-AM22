package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Command-line entry point for launching a MESOS network client.
 *
 * <p>The launcher prompts the user to choose the transport (Socket or RMI),
 * asks for connection parameters, builds the matching
 * {@link ObservableServerConnection} and wraps it in a {@link ClientController}.
 * It then starts a minimal TUI command loop so the whole lobby/game flow can
 * be exercised without a GUI (useful for demos, integration tests and when
 * verifying that Socket and RMI clients can coexist in the same match).
 *
 * <p>Typical usage:
 * <pre>{@code
 *   java it.polimi.ingsw.am22.network.client.NetworkClientLauncher
 * }</pre>
 *
 * <p>Available TUI commands once connected:
 * <ul>
 *   <li>{@code join <nickname>}</li>
 *   <li>{@code expected <n>}</li>
 *   <li>{@code leave}</li>
 *   <li>{@code place <offerLetter>}</li>
 *   <li>{@code pick <cardId>[,<cardId>...]} </li>
 *   <li>{@code bonus <cardId>}</li>
 *   <li>{@code disconnect}</li>
 *   <li>{@code help}</li>
 *   <li>{@code quit}</li>
 * </ul>
 */
public final class NetworkClientLauncher {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_SOCKET_PORT = 12345;
    private static final int DEFAULT_RMI_PORT = 1099;
    private static final String DEFAULT_RMI_BINDING = "MESOS_SERVER";

    private NetworkClientLauncher() {
    }

    public static void main(String[] args) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("=== MESOS Network Client ===");

        Transport transport = askTransport(in);
        if (transport == null) {
            System.out.println("Aborted.");
            return;
        }

        try (ObservableServerConnection connection = openConnection(transport, in)) {
            ClientController controller = new ClientController(connection);
            connection.setClientUpdateHandler(new ConsoleUpdateHandler());

            System.out.println("Connected via " + transport + ". Type 'help' for commands.");
            runCommandLoop(in, controller);
        } catch (IOException e) {
            System.err.println("Socket connection failed: " + e.getMessage());
        } catch (NotBoundException e) {
            System.err.println("RMI binding not found: " + e.getMessage());
        } catch (RemoteException e) {
            System.err.println("RMI error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------

    private static Transport askTransport(BufferedReader in) {
        while (true) {
            String answer = prompt(in, "Choose transport [socket/rmi] (default: socket): ");
            if (answer == null) {
                return null;
            }
            if (answer.isBlank() || answer.equalsIgnoreCase("socket") || answer.equals("1")) {
                return Transport.SOCKET;
            }
            if (answer.equalsIgnoreCase("rmi") || answer.equals("2")) {
                return Transport.RMI;
            }
            System.out.println("  Please answer 'socket' or 'rmi'.");
        }
    }

    private static ObservableServerConnection openConnection(Transport transport, BufferedReader in)
            throws IOException, NotBoundException {
        String host = promptWithDefault(in, "Server host", DEFAULT_HOST);
        switch (transport) {
            case SOCKET: {
                int port = promptIntWithDefault(in, "Socket port", DEFAULT_SOCKET_PORT);
                return new SocketServerConnection(host, port);
            }
            case RMI: {
                int port = promptIntWithDefault(in, "RMI registry port", DEFAULT_RMI_PORT);
                String binding = promptWithDefault(in, "RMI binding name", DEFAULT_RMI_BINDING);
                return new RmiServerConnection(host, port, binding);
            }
            default:
                throw new IllegalStateException("Unsupported transport: " + transport);
        }
    }

    // ---------------------------------------------------------------------
    // TUI command loop
    // ---------------------------------------------------------------------

    private static void runCommandLoop(BufferedReader in, ClientController controller) {
        printHelp();
        while (true) {
            String line = prompt(in, "> ");
            if (line == null) {
                return;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String[] tokens = trimmed.split("\\s+", 2);
            String cmd = tokens[0].toLowerCase(Locale.ROOT);
            String rest = tokens.length > 1 ? tokens[1].trim() : "";

            try {
                switch (cmd) {
                    case "help":
                        printHelp();
                        break;
                    case "quit":
                    case "exit":
                        if (controller.hasJoinedLobby()) {
                            try {
                                controller.disconnect();
                            } catch (Exception ignored) {
                            }
                        }
                        System.out.println("Bye.");
                        return;
                    case "join":
                        requireArg(rest, "join <nickname>");
                        controller.addPlayerToLobby(rest);
                        break;
                    case "expected":
                        requireArg(rest, "expected <n>");
                        controller.setExpectedPlayers(Integer.parseInt(rest));
                        break;
                    case "leave":
                        controller.removePlayerFromLobby();
                        break;
                    case "place":
                        requireArg(rest, "place <offerLetter>");
                        if (rest.length() != 1) {
                            throw new IllegalArgumentException("offerLetter must be a single character.");
                        }
                        controller.placeTotem(rest.charAt(0));
                        break;
                    case "pick":
                        requireArg(rest, "pick <cardId>[,<cardId>...]");
                        List<String> ids = new ArrayList<>(Arrays.asList(rest.split("\\s*,\\s*")));
                        ids.removeIf(String::isBlank);
                        controller.pickCards(ids);
                        break;
                    case "bonus":
                        requireArg(rest, "bonus <cardId>");
                        controller.pickBonusCard(rest);
                        break;
                    case "disconnect":
                        controller.disconnect();
                        break;
                    default:
                        System.out.println("Unknown command: " + cmd + " (type 'help')");
                }
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.err.println("  " + e.getMessage());
            } catch (NumberFormatException e) {
                System.err.println("  Invalid number: " + e.getMessage());
            } catch (Exception e) {
                System.err.println("  Command failed: " + e.getMessage());
            }
        }
    }

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("  join <nickname>                 - join the lobby");
        System.out.println("  expected <n>                    - set expected players (2-4)");
        System.out.println("  leave                           - leave the lobby");
        System.out.println("  place <offerLetter>             - place your totem on the offer tile");
        System.out.println("  pick <cardId>[,<cardId>...]     - pick cards from the selected offer");
        System.out.println("  bonus <cardId>                  - pick the end-of-round bonus card");
        System.out.println("  disconnect                      - notify the server and disconnect");
        System.out.println("  help                            - show this help");
        System.out.println("  quit                            - disconnect and exit");
    }

    private static void requireArg(String value, String usage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }

    // ---------------------------------------------------------------------
    // IO helpers
    // ---------------------------------------------------------------------

    private static String prompt(BufferedReader in, String label) {
        System.out.print(label);
        try {
            return in.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    private static String promptWithDefault(BufferedReader in, String label, String defaultValue) {
        String answer = prompt(in, label + " (default: " + defaultValue + "): ");
        return (answer == null || answer.isBlank()) ? defaultValue : answer.trim();
    }

    private static int promptIntWithDefault(BufferedReader in, String label, int defaultValue) {
        while (true) {
            String answer = prompt(in, label + " (default: " + defaultValue + "): ");
            if (answer == null || answer.isBlank()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(answer.trim());
            } catch (NumberFormatException e) {
                System.out.println("  Please enter a valid integer.");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Handler
    // ---------------------------------------------------------------------

    /**
     * Minimal console-based update handler: prints server messages as they
     * arrive. Real GUIs/TUIs would replace this with a handler that forwards
     * the messages to the client-side Model and refreshes the View.
     */
    private static final class ConsoleUpdateHandler implements ClientUpdateHandler {
        @Override
        public void onServerMessage(ServerMessage message) {
            System.out.println();
            System.out.println("[server] " + message);
            System.out.print("> ");
        }

        @Override
        public void onConnectionClosed(Throwable cause) {
            System.out.println();
            if (cause == null) {
                System.out.println("[server] Connection closed.");
            } else {
                System.out.println("[server] Connection lost: " + cause.getMessage());
            }
        }
    }

    private enum Transport {
        SOCKET, RMI;

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
