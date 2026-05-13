package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/** Server-side abstraction of a single client channel (send/close + binding state). */
public interface ClientChannel {

    void send(ServerMessage message);

    void close();

    String getBoundNickname();

    void setBoundNickname(String nickname);

    /** Match this channel is currently bound to, or {@code null} if not in a match. */
    String getBoundMatchId();

    void setBoundMatchId(String matchId);
}
