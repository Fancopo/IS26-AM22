package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Richiesta di piazzamento del totem su una tessera offerta.
 *
 * @param playerNickname nickname del giocatore che compie l'azione
 * @param offerLetter    lettera della tessera scelta (es. 'A', 'B', ...)
 */
public record PlaceTotemRequest(String playerNickname, char offerLetter) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
