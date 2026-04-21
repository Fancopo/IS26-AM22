package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.rmi.RemoteException;

/**
 * Implementazione di {@link ClientChannel} basata su callback RMI.
 *
 * Wrappa il {@link RemoteClientView} esportato dal client: ogni invio
 * si traduce in una chiamata remota {@code receive()} verso il client.
 * A differenza del canale socket, qui non c'è un socket da chiudere:
 * {@link #close()} è un no-op perché lo stub appartiene al client.
 */
public class RmiClientChannel implements ClientChannel {
    private final RemoteClientView remoteClientView;
    private volatile String boundNickname;

    /**
     * @param remoteClientView callback esportato dal client a cui recapitare i messaggi
     */
    public RmiClientChannel(RemoteClientView remoteClientView) {
        this.remoteClientView = remoteClientView;
        this.boundNickname = null;
    }

    /**
     * Recapita il messaggio al client invocando {@code receive()} via RMI.
     *
     * @param message messaggio da consegnare
     * @throws IllegalStateException se la chiamata RMI fallisce
     */
    @Override
    public void send(ServerMessage message) {
        try {
            remoteClientView.receive(message);
        } catch (RemoteException e) {
            throw new IllegalStateException("Unable to deliver the RMI callback to the client.", e);
        }
    }

    /** No-op: lo stub è gestito dal client, non c'è nulla da chiudere lato server. */
    @Override
    public void close() {
        // Nothing to close explicitly on the server side for an RMI callback stub.
    }

    @Override
    public String getBoundNickname() {
        return boundNickname;
    }

    @Override
    public void setBoundNickname(String nickname) {
        this.boundNickname = nickname;
    }
}
