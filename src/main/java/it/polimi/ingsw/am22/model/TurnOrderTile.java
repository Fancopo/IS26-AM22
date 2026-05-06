package it.polimi.ingsw.am22.model;

import java.util.ArrayList;
import java.util.List;

public class TurnOrderTile {
    private List<Slot> slots;

    public TurnOrderTile() {
        this.slots = new ArrayList<>();
    }

    /**
     * Per-player-count food bonus row for the turn-order tile, as printed on the
     * physical board. Index = slot position - 1. The last slot always carries
     * {@code -1}: the actual deduction is handled by {@link Slot#isLastSpace()} in
     * {@code ActionResolutionState} (which subtracts 1 food, falling back to -2 PP
     * if the player has none), so the value is shown to the user but is never
     * re-applied by the food-bonus path (which only adds when the bonus is strictly
     * positive).
     *
     * <p>Only 2..5 players are supported — the lobby controller already enforces
     * that range, anything else is a programming error.
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
        this.slots.clear();
        int[] bonuses = foodBonusesFor(playerCount);

        for (int i = 0; i < playerCount; i++) {
            int positionIndex = i + 1;
            int foodBonus = bonuses[i];

            // last slot carries a penalty
            boolean isLastSpace = (i == playerCount - 1);

            Slot newSlot = new Slot(foodBonus, positionIndex, isLastSpace);
            this.slots.add(newSlot);
        }
    }

    public Slot getFirstAvailableSlot() {
        for (Slot slot : slots) {
            if (slot.isEmpty()) {
                return slot;
            }
        }
        return null; // returns null if all slots are occupied
    }

    public List<Totem> getTurnOrder() {
        List<Totem> turnOrder = new ArrayList<>();
        for (Slot slot : slots) {
            if (!slot.isEmpty()) {
                turnOrder.add(slot.getOccupiedBy());
            }
        }
        return turnOrder;
    }
    public int getOccupiedSlotsCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (!slot.isEmpty()) {
                count++;
            }
        }
        return count;
    }
    public List<Slot> getSlots(){return this.slots;}
}
