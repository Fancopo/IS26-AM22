package it.polimi.ingsw.am22.model;

import java.util.ArrayList;
import java.util.List;

public class TurnOrderTile {
    private List<Slot> slots;

    public TurnOrderTile() {
        this.slots = new ArrayList<>();
    }

    public void setup(int playerCount) {
        this.slots.clear();

        for (int i = 0; i < playerCount; i++) {
            int positionIndex = i + 1;
            int foodBonus = 0;

            // l'ultimo slot ha penalità
            boolean isLastSpace = (i == playerCount - 1);

            Slot newSlot = new Slot(foodBonus,positionIndex,isLastSpace);
            this.slots.add(newSlot);
        }
    }

    public Slot getFirstAvailableSlot() {
        for (Slot slot : slots) {
            if (slot.isEmpty()) {
                return slot;
            }
        }
        return null; // Restituisce null se tutti gli slot sono occupati
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
