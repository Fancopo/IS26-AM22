package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Richiesta (tipicamente dell'host) di impostare il numero di giocatori attesi.
 *
 * Al raggiungimento di questo numero la partita viene avviata dal server.
 *
 * @param requesterNickname nickname di chi fa la richiesta
 * @param expectedPlayers   numero totale di giocatori desiderato
 */
public record SetExpectedPlayersRequest(String requesterNickname, int expectedPlayers) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
