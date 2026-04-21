package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

/**
 * Richiesta di rimozione di un giocatore dalla lobby (uscita volontaria).
 *
 * @param nickname nickname del giocatore da rimuovere
 */
public record RemovePlayerFromLobbyRequest(String nickname) implements ClientRequest {
}
