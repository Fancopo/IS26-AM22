package it.polimi.ingsw.am22;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TotemTest {

    @Test
    void shouldInitializeTotemCorrectly() {
        Player owner = mock(Player.class);

        Totem totem = new Totem("red", owner);

        assertEquals("red", totem.getColor());
        assertEquals(owner, totem.getOwner());
        assertNull(totem.getCurrentOfferTile());
        assertNull(totem.getCurrentSlot());
        assertFalse(totem.isOnOfferTrack());
        assertFalse(totem.isOnTurnOrderTrack());
    }

    @Test
    void constructorShouldRejectNullColor() {
        Player owner = mock(Player.class);

        assertThrows(IllegalArgumentException.class, () -> new Totem(null, owner));
    }

    @Test
    void constructorShouldRejectBlankColor() {
        Player owner = mock(Player.class);

        assertThrows(IllegalArgumentException.class, () -> new Totem("   ", owner));
    }

    @Test
    void constructorShouldRejectNullOwner() {
        assertThrows(IllegalArgumentException.class, () -> new Totem("red", null));
    }
    @Test
    void moveToTurnOrderShouldRejectNullSlot() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        assertThrows(IllegalArgumentException.class, () -> totem.moveToTurnOrder(null));
    }

    @Test
    void moveToTurnOrderShouldRejectOccupiedSlot() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);
        Slot slot = mock(Slot.class);

        when(slot.isEmpty()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> totem.moveToTurnOrder(slot));
    }

    @Test
    void moveToTurnOrderShouldPlaceTotemInEmptySlot() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);
        Slot slot = mock(Slot.class);

        when(slot.isEmpty()).thenReturn(true);

        totem.moveToTurnOrder(slot);

        assertEquals(slot, totem.getCurrentSlot());
        assertNull(totem.getCurrentOfferTile());
        assertTrue(totem.isOnTurnOrderTrack());
        assertFalse(totem.isOnOfferTrack());
        verify(slot).placeTotem(totem);
    }

    @Test
    void moveToTurnOrderShouldClearPreviousOfferTile() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        OfferTile oldTile = mock(OfferTile.class);
        when(oldTile.isAvailable()).thenReturn(true);

        Slot newSlot = mock(Slot.class);
        when(newSlot.isEmpty()).thenReturn(true);

        totem.moveToOffer(oldTile);
        totem.moveToTurnOrder(newSlot);

        verify(oldTile).clear();
        verify(newSlot).placeTotem(totem);
        assertEquals(newSlot, totem.getCurrentSlot());
        assertNull(totem.getCurrentOfferTile());
    }

    @Test
    void moveToTurnOrderShouldRemoveTotemFromPreviousSlot() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        Slot oldSlot = mock(Slot.class);
        when(oldSlot.isEmpty()).thenReturn(true);

        Slot newSlot = mock(Slot.class);
        when(newSlot.isEmpty()).thenReturn(true);

        totem.moveToTurnOrder(oldSlot);
        totem.moveToTurnOrder(newSlot);

        verify(oldSlot).removeTotem();
        verify(newSlot).placeTotem(totem);
        assertEquals(newSlot, totem.getCurrentSlot());
    }

    @Test
    void moveToOfferShouldRejectNullOfferTile() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        assertThrows(IllegalArgumentException.class, () -> totem.moveToOffer(null));
    }

    @Test
    void moveToOfferShouldRejectOccupiedOfferTile() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);
        OfferTile offerTile = mock(OfferTile.class);

        when(offerTile.isAvailable()).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> totem.moveToOffer(offerTile));
    }

    @Test
    void moveToOfferShouldPlaceTotemOnAvailableOfferTile() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);
        OfferTile offerTile = mock(OfferTile.class);

        when(offerTile.isAvailable()).thenReturn(true);

        totem.moveToOffer(offerTile);

        assertEquals(offerTile, totem.getCurrentOfferTile());
        assertNull(totem.getCurrentSlot());
        assertTrue(totem.isOnOfferTrack());
        assertFalse(totem.isOnTurnOrderTrack());
        verify(offerTile).placeTotem(totem);
    }

    @Test
    void moveToOfferShouldClearPreviousOfferTile() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        OfferTile oldTile = mock(OfferTile.class);
        when(oldTile.isAvailable()).thenReturn(true);

        OfferTile newTile = mock(OfferTile.class);
        when(newTile.isAvailable()).thenReturn(true);

        totem.moveToOffer(oldTile);
        totem.moveToOffer(newTile);

        verify(oldTile).clear();
        verify(newTile).placeTotem(totem);
        assertEquals(newTile, totem.getCurrentOfferTile());
    }

    @Test
    void moveToOfferShouldRemoveTotemFromPreviousSlot() {
        Player owner = mock(Player.class);
        Totem totem = new Totem("red", owner);

        Slot slot = mock(Slot.class);
        when(slot.isEmpty()).thenReturn(true);

        OfferTile offerTile = mock(OfferTile.class);
        when(offerTile.isAvailable()).thenReturn(true);

        totem.moveToTurnOrder(slot);
        totem.moveToOffer(offerTile);

        verify(slot).removeTotem();
        verify(offerTile).placeTotem(totem);
        assertEquals(offerTile, totem.getCurrentOfferTile());
        assertNull(totem.getCurrentSlot());
    }
    @Test
    void moveFromOfferToTurnOrderShouldUpdateBothRealObjects() {
        Player owner = new Player("Alice");
        Totem totem = new Totem("red", owner);
        OfferTile offerTile = new OfferTile('A', 1, 1, 0);
        Slot slot = new Slot(0, 1, false);

        totem.moveToOffer(offerTile);
        totem.moveToTurnOrder(slot);

        assertTrue(offerTile.isAvailable());
        assertNull(offerTile.getOccupiedBy());
        assertFalse(slot.isEmpty());
        assertEquals(totem, slot.getOccupiedBy());
        assertNull(totem.getCurrentOfferTile());
        assertEquals(slot, totem.getCurrentSlot());
    }

    @Test
    void moveFromTurnOrderToOfferShouldUpdateBothRealObjects() {
        Player owner = new Player("Alice");
        Totem totem = new Totem("red", owner);
        Slot slot = new Slot(0, 1, false);
        OfferTile offerTile = new OfferTile('B', 1, 1, 0);

        totem.moveToTurnOrder(slot);
        totem.moveToOffer(offerTile);

        assertTrue(slot.isEmpty());
        assertNull(slot.getOccupiedBy());
        assertFalse(offerTile.isAvailable());
        assertEquals(totem, offerTile.getOccupiedBy());
        assertEquals(offerTile, totem.getCurrentOfferTile());
        assertNull(totem.getCurrentSlot());
    }
}