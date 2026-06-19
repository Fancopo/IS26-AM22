package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;
import it.polimi.ingsw.am22.model.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BonusCardSelectionStateTest {

    private TestGame game;
    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player("buyer");
        game = new TestGame(List.of(player));
        game.setState(new BonusCardSelectionState());
    }

    @Test
    void freeCharacterIsAddedToTheTribeAndRoundProceeds() {
        player.addFood(5);
        TribeCharacter character = new TribeCharacter("c1", Era.I, 1, CharacterType.HUNTER, null);
        game.getBoard().getUpperRow().add(character);

        game.pickBonusCard(player, character);

        assertTrue(player.getTribe().getMembers().contains(character));
        assertFalse(game.getBoard().getUpperRow().contains(character), "Card leaves the board");
        assertEquals(5, player.getFood(), "A character bonus pick is free");
        assertInstanceOf(EventResolutionState.class, game.getCurrentState());
        assertEquals(1, game.resolveEventsCalls, "Round wraps up with event resolution");
    }

    @Test
    void buildingBonusStillCostsFood() {
        player.addFood(5);
        Building building = new Building("b1", Era.I, 1, 4, 3, null);
        game.getBoard().getUpperRow().add(building);

        game.pickBonusCard(player, building);

        assertEquals(1, player.getFood(), "Building costs 4 food (no Builder discount)");
        assertTrue(player.getTribe().getBuildings().contains(building));
        assertFalse(game.getBoard().getUpperRow().contains(building));
        assertInstanceOf(EventResolutionState.class, game.getCurrentState());
    }

    @Test
    void cardOutsideTheUpperRowIsRejected() {
        player.addFood(5);
        TribeCharacter notOnBoard = new TribeCharacter("ghost", Era.I, 1, CharacterType.HUNTER, null);

        assertThrows(IllegalArgumentException.class, () -> game.pickBonusCard(player, notOnBoard));
        assertInstanceOf(BonusCardSelectionState.class, game.getCurrentState());
        assertEquals(0, game.resolveEventsCalls);
    }

    @Test
    void eventCardCannotBeTakenAsBonus() {
        Event event = new Event("e1", Era.I, 1, EventType.HUNTING, null);
        game.getBoard().getUpperRow().add(event);

        assertThrows(IllegalArgumentException.class, () -> game.pickBonusCard(player, event));
        assertTrue(game.getBoard().getUpperRow().contains(event), "Rejected card stays on the board");
        assertEquals(0, game.resolveEventsCalls);
    }

    @Test
    void unaffordableBuildingThrowsAndLeavesEverythingUntouched() {
        player.addFood(3);
        Building expensive = new Building("b1", Era.I, 1, 10, 3, null);
        game.getBoard().getUpperRow().add(expensive);

        assertThrows(IllegalStateException.class, () -> game.pickBonusCard(player, expensive));

        assertEquals(3, player.getFood(), "Food is not spent when validation fails");
        assertTrue(player.getTribe().getBuildings().isEmpty(), "Nothing is committed to the tribe");
        assertTrue(game.getBoard().getUpperRow().contains(expensive), "Card stays on the board");
        assertEquals(0, game.resolveEventsCalls);
    }

    @Test
    void exposesPhaseName() {
        assertEquals("Bonus Card Selection", new BonusCardSelectionState().getPhaseName());
    }

    // ==================== helpers ====================

    /** Game double that records the round-end hand-off instead of running the cascade. */
    private static class TestGame extends Game {
        int resolveEventsCalls = 0;

        TestGame(List<Player> players) { super(players); }

        @Override
        public void resolveEvents() { resolveEventsCalls++; }
    }
}
