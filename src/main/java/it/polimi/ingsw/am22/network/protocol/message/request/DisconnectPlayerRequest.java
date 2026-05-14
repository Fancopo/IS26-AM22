package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Explicit disconnection request from the player.
 *
 * @param matchId  match identifier
 * @param nickname nickname of the disconnecting player
 */
public record DisconnectPlayerRequest(String matchId, String nickname) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
