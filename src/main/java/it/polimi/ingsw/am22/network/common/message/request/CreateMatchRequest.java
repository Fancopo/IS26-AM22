package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;

/**
 * Richiesta di creazione di una nuova partita.
 *
 * Il client fornisce il nickname del creatore (che diventerà host) e il numero
 * di giocatori attesi (2-5). Il server risponde con un {@code MatchJoinedMessage}
 * contenente il matchId appena generato, da usare per le richieste successive.
 */
public record CreateMatchRequest(String hostNickname, int expectedPlayers) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
