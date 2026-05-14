package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request to create a new match.
 *
 * The client provides the creator's nickname (who becomes host) and the expected
 * number of players (2-5). The server replies with a {@code MatchJoinedMessage}
 * carrying the freshly generated matchId, to be used for subsequent requests.
 */
public record CreateMatchRequest(String hostNickname, int expectedPlayers) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
