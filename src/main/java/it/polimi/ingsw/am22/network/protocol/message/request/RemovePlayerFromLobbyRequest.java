package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request to remove a player from the lobby (voluntary leave).
 *
 * @param matchId  match identifier
 * @param nickname nickname of the player to remove
 */
public record RemovePlayerFromLobbyRequest(String matchId, String nickname) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
