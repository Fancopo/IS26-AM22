package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Request to place the totem on an offer tile.
 *
 * @param matchId        match identifier
 * @param playerNickname nickname of the acting player
 * @param offerLetter    letter of the chosen tile (e.g. 'A', 'B', ...)
 */
public record PlaceTotemRequest(String matchId, String playerNickname, char offerLetter) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
