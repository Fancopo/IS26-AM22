package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;

/**
 * Broadcast message with the current match state.
 *
 * Sent on every significant change (player action, turn switch, etc.).
 *
 * @param gameState updated game-state snapshot
 */
public record GameStateMessage(GameStateDTO gameState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
