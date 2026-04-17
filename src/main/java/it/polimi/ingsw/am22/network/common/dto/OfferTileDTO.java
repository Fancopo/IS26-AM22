package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

public record OfferTileDTO(
        char letter,
        int upperCardsToTake,
        int lowerCardsToTake,
        int foodReward,
        String occupiedBy
) implements Serializable {
}
