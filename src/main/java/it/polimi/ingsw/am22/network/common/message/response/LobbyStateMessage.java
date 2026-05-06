package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;

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
