package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

public interface ClientUpdateHandler {
    void onServerMessage(ServerMessage message);

    default void onConnectionClosed(Throwable cause) {
    }
}
