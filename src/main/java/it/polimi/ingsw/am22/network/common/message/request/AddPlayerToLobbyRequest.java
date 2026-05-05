package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Richiesta di ingresso nella lobby di una partita esistente.
 *
 * @param matchId  identificativo della partita a cui unirsi
 * @param nickname nickname scelto dal giocatore
 */
public record AddPlayerToLobbyRequest(String matchId, String nickname) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
