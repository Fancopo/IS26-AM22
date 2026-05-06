package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

/**
 * Broadcast message sent when the match ends.
 *
 * Carries the winner's data and the final game-state snapshot.
 *
 * @param winner         winning player data
 * @param finalGameState final match state
 */
public record EndGameMessage(WinnerDTO winner, GameStateDTO finalGameState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
