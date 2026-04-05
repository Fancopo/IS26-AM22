package it.polimi.ingsw.am22;

public class Slot {
    private int foodBonus;
    private int positionIndex;
    private boolean isLastSpace;
    private Totem occupiedBy;

    public Slot(int foodBonus, int positionIndex, boolean isLastSpace) {
        this.foodBonus = foodBonus;
        this.positionIndex = positionIndex;
        this.isLastSpace = isLastSpace;
        this.occupiedBy = null;
    }

    public boolean isEmpty() {
        return occupiedBy == null;
    }

    public void placeTotem(Totem totem) {
        if (totem == null) {
            throw new IllegalArgumentException("Totem cannot be null.");
        }

        if (!isEmpty()) {
            throw new IllegalStateException("Slot is already occupied.");
        }

        this.occupiedBy = totem;
    }

    public void removeTotem() {
        this.occupiedBy = null;
    }

    public int getFoodBonus() {
        return foodBonus;
    }

    public boolean isLastSpace() {
        return isLastSpace;
    }

    public int getPositionIndex() {
        return positionIndex;
    }

    public Totem getOccupiedBy() {
        return occupiedBy;
    }
}