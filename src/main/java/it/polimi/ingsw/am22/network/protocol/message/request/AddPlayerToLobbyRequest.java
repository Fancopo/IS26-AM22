package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request to join the lobby of an existing match.
 *
 * @param matchId  id of the match to join
 * @param nickname nickname chosen by the player
 */
public record AddPlayerToLobbyRequest(String matchId, String nickname) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
