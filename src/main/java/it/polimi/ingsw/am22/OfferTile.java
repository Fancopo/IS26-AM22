package it.polimi.ingsw.am22;

import java.util.List;

public class OfferTile {
    private char letter;
    private int upperCardsToTake;
    private int lowerCardsToTake;
    private int foodReward;
    private Totem occupiedBy;

    public OfferTile(char letter, int upperCardsToTake, int lowerCardsToTake, int foodReward) {
        this.letter = letter;
        this.upperCardsToTake = upperCardsToTake;
        this.lowerCardsToTake = lowerCardsToTake;
        this.foodReward = foodReward;
        this.occupiedBy = null;
    }

    public boolean isAvailable() {
        return this.occupiedBy == null;
    }

    public void placeTotem(Totem totem) {
        this.occupiedBy = totem;
    }

    public void clear() {
        this.occupiedBy = null;
    }

    // --- Getters ---
    public char getLetter() { return letter; }
    public int getUpperCardsToTake() { return upperCardsToTake; }
    public int getLowerCardsToTake() { return lowerCardsToTake; }
    public int getFoodReward() { return foodReward; }
    public Totem getOccupiedBy() {
        return this.occupiedBy;
    }
}
