package it.polimi.ingsw.am22.network.server.rmi;

import it.polimi.ingsw.am22.controller.server.MatchManager;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.server.ClientHandler;

import java.rmi.RemoteException;

/**
 * {@link ClientHandler} backed by an RMI callback: every send is a remote
 * {@code receive()} call. {@link #close()} is a no-op — the stub belongs to
 * the client, there's nothing to close server-side.
 *
 * <p>When a {@code receive()} call fails with {@link RemoteException} the
 * remote client is no longer reachable (process died, network dropped). To
 * stay symmetric with the socket transport — which calls
 * {@link MatchManager#handleTransportDrop} from its read loop on EOF — this
 * handler escalates the failure to the match manager so the session can
 * tear the player down with {@code transportDrop=true}. Without this, RMI
 * disconnects left the match in a zombie state until the next request from
 * another client triggered a cascade of {@link IllegalStateException}s.
 */
public class RmiClientHandler implements ClientHandler {
    private final RmiClientInterface remoteClientView;
    private final MatchManager matchManager;
    private volatile String boundNickname;
    private volatile String boundMatchId;
    private volatile boolean dropped;

    public RmiClientHandler(RmiClientInterface remoteClientView, MatchManager matchManager) {
        this.remoteClientView = remoteClientView;
        this.matchManager = matchManager;
    }

    @Override
    public void send(ServerMessage message) {
        try {
            remoteClientView.receive(message);
        } catch (RemoteException e) {
            notifyTransportDrop();
            throw new IllegalStateException("Unable to deliver the RMI callback to the client.", e);
        }
    }

    /**
     * Server-driven liveness probe. RMI has no read loop server-side, so a
     * client that dies between outgoing sends is invisible until the next
     * send fails. A periodic ping closes that detection gap. Runs outside
     * any session lock — the call is allowed to block briefly.
     */
    public void probe() {
        if (dropped) return;
        try {
            remoteClientView.ping();
        } catch (RemoteException e) {
            notifyTransportDrop();
        }
    }

    /**
     * Idempotent escalation to the match manager. Only fires once per handler
     * and only when this handler has been bound to a player — ephemeral
     * handlers created for one-shot requests (no nickname yet) are skipped
     * because the match manager has no record of them.
     */
    private void notifyTransportDrop() {
        if (dropped) return;
        dropped = true;
        if (matchManager != null && boundNickname != null && boundMatchId != null) {
            matchManager.handleTransportDrop(this);
        }
    }

    @Override
    public void close() {
        // No-op: the callback stub is owned by the client.
    }

    @Override public String getBoundNickname() { return boundNickname; }
    @Override public void setBoundNickname(String nickname) { this.boundNickname = nickname; }
    @Override public String getBoundMatchId() { return boundMatchId; }
    @Override public void setBoundMatchId(String matchId) { this.boundMatchId = matchId; }
}
