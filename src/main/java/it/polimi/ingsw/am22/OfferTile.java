package it.polimi.ingsw.am22;

import java.util.List;

public class OfferTile {
    private char letter;
    private int upperCardsToTake;
    private int lowerCardsToTake;
    private int foodReward;
    private Totem occupyingTotem;

    public OfferTile(char letter, int upperCardsToTake, int lowerCardsToTake, int foodReward) {
        this.letter = letter;
        this.upperCardsToTake = upperCardsToTake;
        this.lowerCardsToTake = lowerCardsToTake;
        this.foodReward = foodReward;
        this.occupyingTotem = null;
    }

    public boolean isAvailable() {
        return this.occupyingTotem == null;
    }

    public void placeTotem(Totem totem) {
        this.occupyingTotem = totem;
    }

    public void clear() {
        this.occupyingTotem = null;
    }

    // --- Getters ---
    public char getLetter() { return letter; }
    public int getUpperCardsToTake() { return upperCardsToTake; }
    public int getLowerCardsToTake() { return lowerCardsToTake; }
    public int getFoodReward() { return foodReward; }
    public Totem getOccupyingTotem() {
        return this.occupyingTotem;
    }
}
