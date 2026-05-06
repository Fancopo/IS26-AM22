package it.polimi.ingsw.am22.network.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Background watchdog that probes a TCP endpoint on the server while the user
 * is still answering the launch prompts (interface, transport, host, port).
 *
 * <p>Use case: a developer keeps the client window open at the "TUI/GUI" prompt
 * and meanwhile shuts the server down. Without this watchdog the client would
 * sit there forever — there is no actual connection yet, so there is nothing to
 * "drop". The watchdog opens a short-lived socket every {@link #PROBE_INTERVAL_MS}
 * to detect that the server has gone away and fail-fast with a clear message.
 *
 * <p>Behavior:
 * <ul>
 *     <li>Probes are {@link Socket#connect(java.net.SocketAddress, int) connect-with-timeout}
 *         calls (500 ms), then immediately closed. They do not exchange any
 *         payload, so they don't disturb the server.</li>
 *     <li>The watchdog only kills the JVM <strong>after</strong> the server has
 *         been seen alive at least once. This way, if the server is down from
 *         the very start, we leave the door open to the regular
 *         {@code ConnectionFactory.open(...)} error path (or to the initial
 *         fail-fast probe in {@code ClientApp}); we don't override it.</li>
 *     <li>Daemon thread, so it never blocks JVM shutdown on its own.</li>
 * </ul>
 *
 * <p>Once the actual connection is established, callers should invoke
 * {@link #stop()} so we don't keep poking the server in parallel to the live
 * read loop.
 */
public final class ServerWatchdog {

    private static final long PROBE_INTERVAL_MS = 1000;
    private static final int  PROBE_TIMEOUT_MS  = 500;

    // ANSI red wrapper. Inlined here (instead of importing
    // it.polimi.ingsw.am22.view.tui.Ansi) to keep the network.client package
    // free of view-layer dependencies — view.tui already depends on
    // network.client, importing the other direction would close a cycle.
    //  is the ESC byte (0x1B); the rest is the standard SGR color sequence.
    private static final String ANSI_RED   = "[31m";
    private static final String ANSI_RESET = "[0m";

    private final String host;
    private final int port;

    private volatile boolean stopped = false;
    private volatile boolean sawServer = false;
    private Thread thread;

    public ServerWatchdog(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Spawns the watchdog thread. Idempotent: a second call is a no-op. */
    public synchronized void start() {
        if (thread != null) return;
        thread = new Thread(this::loop, "server-watchdog");
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Signals the watchdog to terminate. Safe to call multiple times. Should be
     * invoked as soon as a real connection has been established, so we don't
     * race with the live reader thread for socket accept slots.
     */
    public synchronized void stop() {
        stopped = true;
        if (thread != null) {
            thread.interrupt();
        }
    }

    private void loop() {
        while (!stopped) {
            boolean reachable = probe();
            if (reachable) {
                sawServer = true;
            } else if (sawServer) {
                // The server was up at some point during this client's life and
                // now it isn't anymore. Fail-fast so the user notices immediately
                // even if they're stuck on a blocking Scanner.nextLine().
                // Same message and exit code used by TuiView.onConnectionClosed
                // so the TCP and RMI paths look identical to the user.
                System.out.println(ANSI_RED + "[CONN] Server connection lost  closing client." + ANSI_RESET);
                System.out.println("Bye.");
                System.exit(1);
            }
            try {
                Thread.sleep(PROBE_INTERVAL_MS);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private boolean probe() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
