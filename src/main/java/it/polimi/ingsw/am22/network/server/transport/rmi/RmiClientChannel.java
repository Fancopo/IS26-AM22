package it.polimi.ingsw.am22.network.server.transport.rmi;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.server.transport.ClientChannel;

import java.rmi.RemoteException;

/**
 * {@link ClientChannel} backed by an RMI callback: every send is a remote
 * {@code receive()} call. {@link #close()} is a no-op — the stub belongs to
 * the client, there's nothing to close server-side.
 */
public class RmiClientChannel implements ClientChannel {
    private final RmiClientInterface remoteClientView;
    private volatile String boundNickname;
    private volatile String boundMatchId;

    public RmiClientChannel(RmiClientInterface remoteClientView) {
        this.remoteClientView = remoteClientView;
    }

    @Override
    public void send(ServerMessage message) {
        try {
            remoteClientView.receive(message);
        } catch (RemoteException e) {
            throw new IllegalStateException("Unable to deliver the RMI callback to the client.", e);
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
