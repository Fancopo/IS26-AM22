package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Decorates a {@link ClientHandler} so {@link #send} returns immediately —
 * the actual delivery happens on a per-channel single-thread executor. This
 * has two effects:
 *
 * <ul>
 *   <li>the broadcast loop never blocks on a slow/unreachable peer (each
 *       channel drains independently);</li>
 *   <li>per-channel message ordering is preserved (single-thread executor =
 *       FIFO).</li>
 * </ul>
 *
 * <p>Binding state ({@code boundNickname}/{@code boundMatchId}) and
 * {@link #close()} are delegated to the wrapped handler. {@code close()}
 * uses {@link ExecutorService#shutdown()} so already-queued messages
 * (e.g. {@code EndGameMessage}/{@code MatchClosedMessage}) are still
 * flushed before the underlying transport tears down.
 */
public final class AsyncClientHandler implements ClientHandler {

    private final ClientHandler delegate;
    private final ExecutorService sender;
    private volatile boolean closed;

    public AsyncClientHandler(ClientHandler delegate, String name) {
        this.delegate = delegate;
        this.sender = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "async-send-" + name);
            t.setDaemon(true);
            return t;
        });
    }

    /** The underlying handler, for transport-specific operations (e.g. RMI liveness probe). */
    public ClientHandler unwrap() {
        return delegate;
    }

    @Override
    public void send(ServerMessage message) {
        if (closed) return;
        sender.execute(() -> {
            try {
                delegate.send(message);
            } catch (Exception ignored) {
                // The delegate already escalated the drop to MatchManager.
            }
        });
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        sender.shutdown();
        delegate.close();
    }

    @Override public String getBoundNickname()         { return delegate.getBoundNickname(); }
    @Override public void   setBoundNickname(String n) { delegate.setBoundNickname(n); }
    @Override public String getBoundMatchId()          { return delegate.getBoundMatchId(); }
    @Override public void   setBoundMatchId(String id) { delegate.setBoundMatchId(id); }
}
