package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

public interface ClientChannel {
    void send(ServerMessage message);
    void close();
    String getBoundNickname();
    void setBoundNickname(String nickname);
}
