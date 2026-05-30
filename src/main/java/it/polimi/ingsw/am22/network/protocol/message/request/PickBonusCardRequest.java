package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Player's request to pick a bonus card.
 *
 * @param matchId        match identifier
 * @param playerNickname nickname of the acting player
 * @param bonusCardId    id of the chosen bonus card
 */
public record PickBonusCardRequest(String matchId, String playerNickname, String bonusCardId) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
