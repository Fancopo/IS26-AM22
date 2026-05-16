package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TurnOrderTile implements Serializable {
    private List<Slot> slots;

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

    public void setup(int playerCount) {
        slots.clear();
        int[] bonuses = foodBonusesFor(playerCount);
        for (int i = 0; i < playerCount; i++) {
            boolean isLastSpace = (i == playerCount - 1);
            slots.add(new Slot(bonuses[i], i + 1, isLastSpace));
        }
    }

    public Slot getFirstAvailableSlot() {
        for (Slot slot : slots) {
            if (slot.isEmpty()) return slot;
        }
        return null;
    }

    public List<Totem> getTurnOrder() {
        List<Totem> turnOrder = new ArrayList<>();
        for (Slot slot : slots) {
            if (!slot.isEmpty()) turnOrder.add(slot.getOccupiedBy());
        }
        return turnOrder;
    }

    public int getOccupiedSlotsCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (!slot.isEmpty()) count++;
        }
        return count;
    }

    public List<Slot> getSlots() { return slots; }
}
