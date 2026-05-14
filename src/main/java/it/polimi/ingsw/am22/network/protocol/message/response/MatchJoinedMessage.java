package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

/**
 * Confirmation sent to the client after creating or joining a match.
 *
 * The client must store {@code matchId} and attach it to all subsequent requests.
 */
public record MatchJoinedMessage(String matchId, String nickname) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
