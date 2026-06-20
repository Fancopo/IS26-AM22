package it.polimi.ingsw.am22.model;

import java.io.Serializable;

/**
 * A single space on the {@link TurnOrderTile}. It carries the food bonus (or
 * penalty) for landing there and tracks which totem, if any, currently occupies
 * it.
 */
public class Slot implements Serializable {
    private int foodBonus;
    private int positionIndex;
    private boolean isLastSpace;
    private Totem occupiedBy;

    /**
     * @param foodBonus     the food granted when a totem lands on this slot
     * @param positionIndex the 1-based position of this slot on the tile
     * @param isLastSpace   whether this is the last (penalty) space
     */
    public Slot(int foodBonus, int positionIndex, boolean isLastSpace) {
        this.foodBonus = foodBonus;
        this.positionIndex = positionIndex;
        this.isLastSpace = isLastSpace;
        this.occupiedBy = null;
    }

    /** @return {@code true} if no totem occupies this slot */
    public boolean isEmpty() {
        return occupiedBy == null;
    }

    /**
     * Places a totem on this slot.
     *
     * @param totem the totem to place
     * @throws IllegalArgumentException if {@code totem} is null
     * @throws IllegalStateException    if the slot is already occupied
     */
    public void placeTotem(Totem totem) {
        if (totem == null) {
            throw new IllegalArgumentException("Totem cannot be null.");
        }

        if (!isEmpty()) {
            throw new IllegalStateException("Slot is already occupied.");
        }

        this.occupiedBy = totem;
    }

    /** Removes the totem currently on this slot, if any. */
    public void removeTotem() {
        this.occupiedBy = null;
    }

    /** @return the food granted when a totem lands on this slot */
    public int getFoodBonus() {
        return foodBonus;
    }

    /** @return whether this is the last (penalty) space */
    public boolean isLastSpace() {
        return isLastSpace;
    }

    /** @return the 1-based position of this slot on the tile */
    public int getPositionIndex() {
        return positionIndex;
    }

    /** @return the totem occupying this slot, or {@code null} if empty */
    public Totem getOccupiedBy() {
        return occupiedBy;
    }
}
