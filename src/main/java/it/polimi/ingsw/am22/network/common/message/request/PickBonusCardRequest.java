package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Richiesta di scelta di una carta bonus da parte del giocatore.
 *
 * @param playerNickname nickname del giocatore che compie l'azione
 * @param bonusCardId    id della carta bonus scelta
 */
public record PickBonusCardRequest(String playerNickname, String bonusCardId) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
