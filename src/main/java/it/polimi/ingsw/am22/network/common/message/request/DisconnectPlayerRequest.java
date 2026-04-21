package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

/**
 * Richiesta di disconnessione esplicita da parte del giocatore.
 *
 * @param nickname nickname del giocatore che si disconnette
 */
public record DisconnectPlayerRequest(String nickname) implements ClientRequest {
}
