package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;

public record OfferTileView(
        char letter,
        int upperCardsToTake,
        int lowerCardsToTake,
        int foodReward,
        String occupiedBy
) implements Serializable {
}
