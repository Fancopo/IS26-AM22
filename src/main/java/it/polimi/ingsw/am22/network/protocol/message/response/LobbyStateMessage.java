package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.dto.LobbyStateDTO;

/**
 * Broadcast message with the updated lobby state.
 *
 * Sent on every join/leave or configuration change
 * (e.g. expected player count) before the match starts.
 *
 * @param lobbyState lobby-state snapshot
 */
public record LobbyStateMessage(LobbyStateDTO lobbyState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
