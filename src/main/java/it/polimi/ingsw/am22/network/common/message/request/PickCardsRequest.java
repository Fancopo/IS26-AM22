package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.util.List;

/**
 * Richiesta di raccolta di una o più carte dalla board.
 *
 * Il costruttore compatto rende la lista immutabile e sostituisce
 * {@code null} con una lista vuota, evitando controlli lato ricevente.
 *
 * @param playerNickname  nickname del giocatore che compie l'azione
 * @param selectedCardIds lista (immutabile) degli id delle carte scelte
 */
public record PickCardsRequest(String playerNickname, List<String> selectedCardIds) implements ClientRequest {
    public PickCardsRequest {
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
    }
}
