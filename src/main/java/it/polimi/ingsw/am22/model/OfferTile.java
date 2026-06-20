package it.polimi.ingsw.am22.model;

import java.io.Serializable;

/**
 * A tile on the offer track. A player places their totem on a tile to claim its
 * action: take a fixed number of cards from the upper and/or lower row and/or
 * gain food. Each tile is identified by a letter and can hold a single totem.
 */
public class OfferTile implements Serializable {
    private char letter;
    private int upperCardsToTake;
    private int lowerCardsToTake;
    private int foodReward;
    private Totem occupiedBy;

    /**
     * @param letter           the tile's identifying letter
     * @param upperCardsToTake how many cards must be taken from the upper row
     * @param lowerCardsToTake how many cards must be taken from the lower row
     * @param foodReward       the food granted by this tile
     */
    public OfferTile(char letter, int upperCardsToTake, int lowerCardsToTake, int foodReward) {
        this.letter = letter;
        this.upperCardsToTake = upperCardsToTake;
        this.lowerCardsToTake = lowerCardsToTake;
        this.foodReward = foodReward;
        this.occupiedBy = null;
    }

    /** @return {@code true} if no totem occupies this tile */
    public boolean isAvailable() {
        return this.occupiedBy == null;
    }

    /**
     * Places a totem on this tile.
     *
     * @param totem the totem to place
     */
    public void placeTotem(Totem totem) {
        this.occupiedBy = totem;
    }

    /** Clears the totem from this tile, making it available again. */
    public void clear() {
        this.occupiedBy = null;
    }

    // --- Getters ---

    /** @return the tile's identifying letter */
    public char getLetter() { return letter; }

    /** @return how many cards must be taken from the upper row */
    public int getUpperCardsToTake() { return upperCardsToTake; }

    /** @return how many cards must be taken from the lower row */
    public int getLowerCardsToTake() { return lowerCardsToTake; }

    /** @return the food granted by this tile */
    public int getFoodReward() { return foodReward; }

    /** @return the totem on this tile, or {@code null} if available */
    public Totem getOccupiedBy() {
        return this.occupiedBy;
    }
}
