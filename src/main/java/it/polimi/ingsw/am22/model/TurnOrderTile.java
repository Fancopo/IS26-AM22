package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The turn-order tile: the ordered row of {@link Slot}s that totems return to
 * after acting during a round. The slot a totem lands on determines both the
 * next round's turn order and a food bonus (or, on the last space, a penalty).
 */
public class TurnOrderTile implements Serializable {
    private List<Slot> slots;

    /** Creates an empty turn-order tile; call {@link #setup(int)} before use. */
    public TurnOrderTile() {
        this.slots = new ArrayList<>();
    }

    /**
     * Food bonus row printed on the physical tile, per player count.
     * The trailing -1 is just informative: the actual deduction on the last
     * slot is handled via {@link Slot#isLastSpace()} (-1 food, falling back
     * to -2 PP if the player has none), so this column is never re-applied
     * by the positive-bonus path.
     */
    private static int[] foodBonusesFor(int playerCount) {
        return switch (playerCount) {
            case 2 -> new int[]{ 1, -1 };
            case 3 -> new int[]{ 2, 0, -1 };
            case 4 -> new int[]{ 2, 1, 0, -1 };
            case 5 -> new int[]{ 3, 1, 0, 0, -1 };
            default -> throw new IllegalArgumentException(
                    "Unsupported player count for turn order tile: " + playerCount);
        };
    }

    /**
     * (Re)builds the slots for the given number of players, applying the printed
     * food bonuses and marking the last space as the penalty space.
     *
     * @param playerCount the number of players (2-5)
     * @throws IllegalArgumentException if {@code playerCount} is unsupported
     */
    public void setup(int playerCount) {
        slots.clear();
        int[] bonuses = foodBonusesFor(playerCount);
        for (int i = 0; i < playerCount; i++) {
            boolean isLastSpace = (i == playerCount - 1);
            slots.add(new Slot(bonuses[i], i + 1, isLastSpace));
        }
    }

    /** @return the first empty slot in order, or {@code null} if all are occupied */
    public Slot getFirstAvailableSlot() {
        for (Slot slot : slots) {
            if (slot.isEmpty()) return slot;
        }
        return null;
    }

    /** @return the totems currently on the tile, in slot order (i.e. the new turn order) */
    public List<Totem> getTurnOrder() {
        List<Totem> turnOrder = new ArrayList<>();
        for (Slot slot : slots) {
            if (!slot.isEmpty()) turnOrder.add(slot.getOccupiedBy());
        }
        return turnOrder;
    }

    /** @return how many slots are currently occupied */
    public int getOccupiedSlotsCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (!slot.isEmpty()) count++;
        }
        return count;
    }

    /** @return the slots of this tile, in order */
    public List<Slot> getSlots() { return slots; }
}
