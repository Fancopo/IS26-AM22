package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.rmi.RemoteException;

public class RmiClientChannel implements ClientChannel {
    private final RemoteClientView remoteClientView;
    private volatile String boundNickname;

    public RmiClientChannel(RemoteClientView remoteClientView) {
        this.remoteClientView = remoteClientView;
        this.boundNickname = null;
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
