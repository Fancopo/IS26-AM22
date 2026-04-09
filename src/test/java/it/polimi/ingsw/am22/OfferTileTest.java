package it.polimi.ingsw.am22;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class OfferTileTest {

    @Test
    void shouldInitializeOfferTileCorrectly() {
        OfferTile tile = new OfferTile('A', 1, 2, 3);

        assertEquals('A', tile.getLetter());
        assertEquals(1, tile.getUpperCardsToTake());
        assertEquals(2, tile.getLowerCardsToTake());
        assertEquals(3, tile.getFoodReward());
        assertTrue(tile.isAvailable());
        assertNull(tile.getOccupiedBy());
    }
    @Test
    void shouldAllowPlacingAnotherTotemAfterClear() {
        OfferTile tile = new OfferTile('Z', 1, 1, 0);
        Totem firstTotem = mock(Totem.class);
        Totem secondTotem = mock(Totem.class);

        tile.placeTotem(firstTotem);
        tile.clear();
        tile.placeTotem(secondTotem);

        assertFalse(tile.isAvailable());
        assertEquals(secondTotem, tile.getOccupiedBy());
    }

    @Test
    void placeTotemShouldAllowNullAndKeepTileFree() {
        OfferTile tile = new OfferTile('B', 1, 1, 0);

        tile.placeTotem(null);

        assertTrue(tile.isAvailable());
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void placeTotemShouldPlaceTotemWhenTileIsFree() {
        OfferTile tile = new OfferTile('C', 0, 1, 2);
        Totem totem = mock(Totem.class);

        tile.placeTotem(totem);

        assertFalse(tile.isAvailable());
        assertEquals(totem, tile.getOccupiedBy());
    }

    @Test
    void placeTotemShouldReplacePreviousTotem() {
        OfferTile tile = new OfferTile('D', 2, 0, 1);
        Totem firstTotem = mock(Totem.class);
        Totem secondTotem = mock(Totem.class);

        tile.placeTotem(firstTotem);
        tile.placeTotem(secondTotem);

        assertFalse(tile.isAvailable());
        assertEquals(secondTotem, tile.getOccupiedBy());
    }
    @Test
    void clearShouldFreeTheTile() {
        OfferTile tile = new OfferTile('E', 1, 1, 1);
        Totem totem = mock(Totem.class);

        tile.placeTotem(totem);
        tile.clear();

        assertTrue(tile.isAvailable());
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void clearShouldDoNothingIfTileIsAlreadyFree() {
        OfferTile tile = new OfferTile('F', 0, 2, 0);

        tile.clear();

        assertTrue(tile.isAvailable());
        assertNull(tile.getOccupiedBy());
    }

    @Test
    void isAvailableShouldReturnFalseWhenTotemIsPresent() {
        OfferTile tile = new OfferTile('G', 1, 0, 0);
        Totem totem = mock(Totem.class);

        tile.placeTotem(totem);

        assertFalse(tile.isAvailable());
    }

    @Test
    void isAvailableShouldReturnTrueAfterClear() {
        OfferTile tile = new OfferTile('H', 1, 0, 0);
        Totem totem = mock(Totem.class);

        tile.placeTotem(totem);
        tile.clear();

        assertTrue(tile.isAvailable());
    }
}