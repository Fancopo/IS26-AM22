package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.OfferTile;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Slot;
import it.polimi.ingsw.am22.model.Totem;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;
import it.polimi.ingsw.am22.model.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ActionResolutionStateTest {

    private TestGame game;
    private Player actor;
    private Player other;

    @BeforeEach
    void setUp() {
        actor = new Player("actor");
        actor.setTotem(new Totem("red", actor));
        other = new Player("other");
        other.setTotem(new Totem("blue", other));
        game = new TestGame(List.of(actor, other));
        game.setState(new ActionResolutionState());
        game.setActivePlayer(actor);
    }

    @Test
    void pickingWhileNotOnTheOfferTrackThrows() {
        // actor's totem was never placed on a tile
        assertThrows(IllegalStateException.class, () -> game.pickCards(actor, List.of()));
    }

    @Test
    void foodOnlyTileGrantsFoodForAnEmptySelection() {
        placeActorOnTile('A', 0, 0, 3);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));

        game.pickCards(actor, List.of());

        assertEquals(3, actor.getFood());
    }

    @Test
    void mandatoryCharacterIsMovedIntoTheTribe() {
        placeActorOnTile('C', 1, 0, 0);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        TribeCharacter character = new TribeCharacter("c1", Era.I, 1, CharacterType.HUNTER, null);
        game.getBoard().getUpperRow().add(character);

        game.pickCards(actor, List.of(character));

        assertTrue(actor.getTribe().getMembers().contains(character));
        assertTrue(game.getBoard().getUpperRow().isEmpty(), "Card is removed from the board");
    }

    @Test
    void affordableBuildingIsPurchasedAndPaidFor() {
        placeActorOnTile('C', 1, 0, 0);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        actor.addFood(5);
        Building building = new Building("b1", Era.I, 1, 4, 3, null);
        game.getBoard().getUpperRow().add(building);

        game.pickCards(actor, List.of(building));

        assertEquals(1, actor.getFood(), "5 food minus the 4 cost");
        assertTrue(actor.getTribe().getBuildings().contains(building));
        assertTrue(game.getBoard().getUpperRow().isEmpty());
    }

    @Test
    void unaffordableBuildingFailsWithoutMutatingAnything() {
        OfferTile tile = placeActorOnTile('C', 1, 0, 0);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        actor.addFood(5);
        Building expensive = new Building("b1", Era.I, 1, 10, 3, null);
        game.getBoard().getUpperRow().add(expensive);

        assertThrows(IllegalStateException.class, () -> game.pickCards(actor, List.of(expensive)));

        assertEquals(5, actor.getFood(), "No food spent");
        assertTrue(actor.getTribe().getBuildings().isEmpty(), "Nothing committed to the tribe");
        assertTrue(game.getBoard().getUpperRow().contains(expensive), "Card stays on the board");
        assertSame(tile, actor.getTotem().getCurrentOfferTile(), "Totem has not moved off the offer track");
    }

    @Test
    void selectingAnEventIsRejected() {
        OfferTile tile = placeActorOnTile('C', 1, 0, 0);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        Event event = new Event("e1", Era.I, 1, EventType.HUNTING, null);
        game.getBoard().getUpperRow().add(event);

        assertThrows(IllegalArgumentException.class, () -> game.pickCards(actor, List.of(event)));

        assertTrue(game.getBoard().getUpperRow().contains(event));
        assertSame(tile, actor.getTotem().getCurrentOfferTile());
    }

    @Test
    void skippingAMandatoryCardIsRejected() {
        OfferTile tile = placeActorOnTile('C', 1, 0, 0);
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        TribeCharacter character = new TribeCharacter("c1", Era.I, 1, CharacterType.HUNTER, null);
        game.getBoard().getUpperRow().add(character);

        // The tile demands one upper card and a mandatory character is available
        assertThrows(IllegalArgumentException.class, () -> game.pickCards(actor, List.of()));

        assertTrue(game.getBoard().getUpperRow().contains(character));
        assertSame(tile, actor.getTotem().getCurrentOfferTile());
    }

    @Test
    void lastSpacePenaltyCostsOneFoodWhenAvailable() {
        placeActorOnTile('Z', 0, 0, 0);
        addSlots(new Slot(0, 1, true), new Slot(0, 2, false)); // destination is the last space
        actor.addFood(2);

        game.pickCards(actor, List.of());

        assertEquals(1, actor.getFood(), "Landing on the last space costs 1 food");
    }

    @Test
    void lastSpacePenaltyCostsTwoPointsWhenNoFood() {
        placeActorOnTile('Z', 0, 0, 0);
        addSlots(new Slot(0, 1, true), new Slot(0, 2, false));
        // actor has 0 food

        game.pickCards(actor, List.of());

        assertEquals(-2, actor.getPP(), "With no food the penalty is 2 points");
        assertEquals(0, actor.getFood());
    }

    @Test
    void turnOrderFoodBonusIsAddedAndWakesUpBuildings() {
        placeActorOnTile('Z', 0, 0, 0);
        addSlots(new Slot(2, 1, false), new Slot(0, 2, false)); // destination grants 2 food
        RecordingEffect effect = new RecordingEffect();
        actor.getTribe().addBuilding(new Building("b1", Era.I, 1, 0, 0, effect));

        game.pickCards(actor, List.of());

        assertEquals(2, actor.getFood());
        assertTrue(effect.totemPlacedCalled, "Buildings react to the owner landing on a food slot");
    }

    @Test
    void finishingTheRoundWithNoBonusStartsEventResolution() {
        // other player already back in the turn order -> actor is the last to act
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        other.getTotem().moveToTurnOrder(game.getBoard().getTurnOrderTile().getSlots().get(0));
        placeActorOnTile('Z', 0, 0, 0);

        game.pickCards(actor, List.of());

        assertInstanceOf(EventResolutionState.class, game.getCurrentState());
        assertEquals(1, game.resolveEventsCalls);
    }

    @Test
    void finishingTheRoundWithABonusBuildingEntersBonusSelection() {
        addSlots(new Slot(0, 1, false), new Slot(0, 2, false));
        other.getTotem().moveToTurnOrder(game.getBoard().getTurnOrderTile().getSlots().get(0));
        placeActorOnTile('Z', 0, 0, 0);

        RecordingEffect bonusEffect = new RecordingEffect();
        bonusEffect.extraBuy = true;
        other.getTribe().addBuilding(new Building("bonus", Era.I, 1, 0, 0, bonusEffect));

        game.pickCards(actor, List.of());

        assertInstanceOf(BonusCardSelectionState.class, game.getCurrentState());
        assertSame(other, game.getActivePlayer(), "The owner of the bonus building acts next");
        assertEquals(0, game.resolveEventsCalls, "Events wait until after the bonus pick");
    }

    @Test
    void exposesPhaseName() {
        assertEquals("Action Resolution", new ActionResolutionState().getPhaseName());
    }

    // ==================== helpers ====================

    private OfferTile placeActorOnTile(char letter, int upper, int lower, int food) {
        OfferTile tile = new OfferTile(letter, upper, lower, food);
        game.getBoard().getOfferTrack().add(tile);
        actor.getTotem().moveToOffer(tile);
        return tile;
    }

    private void addSlots(Slot... slots) {
        for (Slot s : slots) {
            game.getBoard().getTurnOrderTile().getSlots().add(s);
        }
    }

    /** Game double that records the round-end hand-off instead of running the cascade. */
    private static class TestGame extends Game {
        int resolveEventsCalls = 0;

        TestGame(List<Player> players) { super(players); }

        @Override
        public void resolveEvents() { resolveEventsCalls++; }
    }

    /** Building effect that records reacting to a food-slot landing and can grant an extra buy. */
    private static class RecordingEffect implements BuildingEffect {
        boolean totemPlacedCalled = false;
        boolean extraBuy = false;

        @Override public void onTotemPlaced(Player owner) { totemPlacedCalled = true; }
        @Override public boolean hasExtraBuyAtRoundEnd() { return extraBuy; }
        @Override public int calculateEndGame(Tribe tribe) { return 0; }
    }
}
