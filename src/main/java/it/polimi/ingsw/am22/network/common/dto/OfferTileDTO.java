package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * DTO serializzabile che rappresenta una tessera offerta sulla board.
 *
 * @param letter            lettera identificativa della tessera
 * @param upperCardsToTake  numero di carte da pescare dalla riga superiore
 * @param lowerCardsToTake  numero di carte da pescare dalla riga inferiore
 * @param foodReward        quantità di cibo ottenuta scegliendo la tessera
 * @param occupiedBy        nickname del giocatore che occupa la tessera ({@code null} se libera)
 */
public record OfferTileDTO(
        char letter,
        int upperCardsToTake,
        int lowerCardsToTake,
        int foodReward,
        String occupiedBy
) implements Serializable {
}
