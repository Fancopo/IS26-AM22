package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

/**
 * Richiesta di ingresso nella lobby.
 *
 * @param nickname nickname scelto dal giocatore
 */
public record AddPlayerToLobbyRequest(String nickname) implements ClientRequest {
}
