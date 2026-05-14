package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request (typically by the host) to set the expected number of players.
 *
 * When that number is reached the server starts the match.
 *
 * @param matchId           match identifier
 * @param requesterNickname nickname of the requester
 * @param expectedPlayers   total desired number of players
 */
public record SetExpectedPlayersRequest(String matchId, String requesterNickname, int expectedPlayers) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
