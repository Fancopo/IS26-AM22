package it.polimi.ingsw.am22.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class SlotTest {

    @Test
    void shouldInitializeSlotCorrectly() {
        Slot slot = new Slot(2, 1, false);

        assertEquals(2, slot.getFoodBonus());
        assertEquals(1, slot.getPositionIndex());
        assertFalse(slot.isLastSpace());
        assertTrue(slot.isEmpty());
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void shouldInitializeLastSpaceSlotCorrectly() {
        Slot slot = new Slot(0, 4, true);

        assertEquals(0, slot.getFoodBonus());
        assertEquals(4, slot.getPositionIndex());
        assertTrue(slot.isLastSpace());
        assertTrue(slot.isEmpty());
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void shouldAllowPlacingAnotherTotemAfterRemoval() {
        Slot slot = new Slot(0, 1, false);
        Totem firstTotem = mock(Totem.class);
        Totem secondTotem = mock(Totem.class);

        slot.placeTotem(firstTotem);
        slot.removeTotem();
        slot.placeTotem(secondTotem);

        assertFalse(slot.isEmpty());
        assertEquals(secondTotem, slot.getOccupiedBy());
    }

    @Test
    void placeTotemShouldRejectNull() {
        Slot slot = new Slot(0, 1, false);

        assertThrows(IllegalArgumentException.class, () -> slot.placeTotem(null));
    }

    @Test
    void placeTotemShouldPlaceTotemInEmptySlot() {
        Slot slot = new Slot(1, 2, false);
        Totem totem = mock(Totem.class);

        slot.placeTotem(totem);

        assertFalse(slot.isEmpty());
        assertEquals(totem, slot.getOccupiedBy());
    }

    @Test
    void placeTotemShouldRejectIfAlreadyOccupied() {
        Slot slot = new Slot(0, 1, false);
        Totem firstTotem = mock(Totem.class);
        Totem secondTotem = mock(Totem.class);

        slot.placeTotem(firstTotem);

        assertThrows(IllegalStateException.class, () -> slot.placeTotem(secondTotem));
        assertEquals(firstTotem, slot.getOccupiedBy());
    }

    @Test
    void removeTotemShouldFreeTheSlot() {
        Slot slot = new Slot(0, 1, false);
        Totem totem = mock(Totem.class);

        slot.placeTotem(totem);
        slot.removeTotem();

        assertTrue(slot.isEmpty());
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void removeTotemShouldDoNothingIfSlotIsAlreadyEmpty() {
        Slot slot = new Slot(0, 1, false);

        slot.removeTotem();

        assertTrue(slot.isEmpty());
        assertNull(slot.getOccupiedBy());
    }

    @Test
    void isEmptyShouldReturnFalseWhenTotemIsPresent() {
        Slot slot = new Slot(0, 1, false);
        Totem totem = mock(Totem.class);

        slot.placeTotem(totem);

        assertFalse(slot.isEmpty());
    }

    @Test
    void isEmptyShouldReturnTrueAfterRemovingTotem() {
        Slot slot = new Slot(0, 1, false);
        Totem totem = mock(Totem.class);

        slot.placeTotem(totem);
        slot.removeTotem();

        assertTrue(slot.isEmpty());
    }
}
