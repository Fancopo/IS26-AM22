package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request sent by a player to pick a totem color during the pre-game totem
 * selection phase. Only the player whose turn it is (lobby join order) may
 * choose, and only a color not yet taken.
 *
 * @param matchId        match identifier
 * @param playerNickname nickname of the choosing player
 * @param color          chosen totem color (e.g. "Red", "Blue", ...)
 */
public record ChooseTotemRequest(String matchId, String playerNickname, String color) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
