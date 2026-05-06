package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;

/**
 * Broadcast message sent the moment the match starts.
 *
 * Lets clients leave the "lobby" state and show the game UI.
 *
 * @param initialGameState initial game-state snapshot
 */
public record GameStartedMessage(GameStateDTO initialGameState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
