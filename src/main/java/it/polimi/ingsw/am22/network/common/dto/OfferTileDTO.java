package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * Serializable DTO representing an offer tile on the board.
 *
 * @param letter            tile identifier letter
 * @param upperCardsToTake  number of cards to draw from the upper row
 * @param lowerCardsToTake  number of cards to draw from the lower row
 * @param foodReward        food gained when picking this tile
 * @param occupiedBy        nickname of the player occupying the tile ({@code null} if free)
 */
public record OfferTileDTO(
        char letter,
        int upperCardsToTake,
        int lowerCardsToTake,
        int foodReward,
        String occupiedBy
) implements Serializable {
}
