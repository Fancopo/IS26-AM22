package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoardTest {

    private Board board;

    @BeforeEach
    void setUp() {
        board = new Board(2);
    }

    // ==================== CONSTRUCTOR ====================

    @Test
    void constructorStartsWithEmptyRowsAndTrack() {
        assertTrue(board.getUpperRow().isEmpty());
        assertTrue(board.getLowerRow().isEmpty());
        assertTrue(board.getOfferTrack().isEmpty());
        assertNotNull(board.getTurnOrderTile());
    }

    // ==================== initTrack ====================

    @Test
    void initTrackForTwoPlayersHasFourTilesSortedByLetter() {
        board.initTrack(2);
        assertEquals(List.of('B', 'C', 'E', 'F'), letters());
    }

    @Test
    void initTrackForThreePlayersAddsTileD() {
        board.initTrack(3);
        assertEquals(List.of('B', 'C', 'D', 'E', 'F'), letters());
    }

    @Test
    void initTrackForFourPlayersAddsTileG() {
        board.initTrack(4);
        assertEquals(List.of('B', 'C', 'D', 'E', 'F', 'G'), letters());
    }

    @Test
    void initTrackForFivePlayersAddsTileA() {
        board.initTrack(5);
        assertEquals(List.of('A', 'B', 'C', 'D', 'E', 'F', 'G'), letters());
    }

    @Test
    void initTrackSetsTileRewardsAndRequirements() {
        board.initTrack(5);
        OfferTile a = tile('A');
        OfferTile b = tile('B');
        OfferTile f = tile('F');

        assertEquals(3, a.getFoodReward(), "Tile A is the food-only tile");
        assertEquals(0, a.getUpperCardsToTake());
        assertEquals(1, b.getLowerCardsToTake(), "Tile B takes one lower card");
        assertEquals(2, f.getUpperCardsToTake(), "Tile F takes two upper cards");
    }

    @Test
    void initTrackIsIdempotentAndDoesNotAccumulate() {
        board.initTrack(2);
        board.initTrack(2);
        assertEquals(4, board.getOfferTrack().size(), "Re-initialising must clear the previous track");
    }

    // ==================== dealInitialCards ====================

    @Test
    void dealInitialCardsFillsBothRowsWhenNoEvents() {
        List<Card> deck = deckOfNonEvents(10);
        board.dealInitialCards(deck, 2);

        assertEquals(3, board.getLowerRow().size(), "lower row = numPlayers + 1");
        assertEquals(6, board.getUpperRow().size(), "upper row topped up to numPlayers + 4");
        assertEquals(1, deck.size(), "9 of the 10 cards were dealt");
    }

    @Test
    void dealInitialCardsDivertsEventsToUpperRow() {
        List<Card> deck = new ArrayList<>();
        deck.add(nonEvent("n1"));
        deck.add(nonEvent("n2"));
        Card event = event("evt");
        deck.add(event);
        for (int i = 3; i <= 12; i++) deck.add(nonEvent("n" + i));

        board.dealInitialCards(deck, 2);

        assertEquals(3, board.getLowerRow().size());
        assertFalse(board.getLowerRow().contains(event), "Events never land in the lower row");
        assertTrue(board.getUpperRow().contains(event), "The event was diverted to the upper row");
    }

    @Test
    void dealInitialCardsStopsWhenDeckRunsOut() {
        List<Card> deck = deckOfNonEvents(2);

        assertDoesNotThrow(() -> board.dealInitialCards(deck, 2));

        assertEquals(2, board.getLowerRow().size());
        assertEquals(0, board.getUpperRow().size());
        assertTrue(deck.isEmpty());
    }

    // ==================== round-end row maintenance ====================

    @Test
    void clearLowerRowRemovesCardsThatDoNotSurvive() {
        Card survivor = card("s", Era.I, true, false, false);
        Card transient_ = card("t", Era.I, false, false, false);
        board.getLowerRow().add(survivor);
        board.getLowerRow().add(transient_);

        board.clearLowerRow();

        assertEquals(List.of(survivor), board.getLowerRow());
    }

    @Test
    void clearLowerBuildingsRemovesCardsDestroyedOnEraIII() {
        Card destroyed = card("d", Era.I, true, true, false);
        Card kept = card("k", Era.I, true, false, false);
        board.getLowerRow().add(destroyed);
        board.getLowerRow().add(kept);

        board.clearLowerBuildings();

        assertEquals(List.of(kept), board.getLowerRow());
    }

    @Test
    void shiftUpToLowMovesNonSurvivingCardsDown() {
        Card survivor = card("s", Era.I, true, false, false);
        Card transient_ = card("t", Era.I, false, false, false);
        board.getUpperRow().add(survivor);
        board.getUpperRow().add(transient_);

        board.shiftUpToLow();

        assertEquals(List.of(survivor), board.getUpperRow());
        assertEquals(List.of(transient_), board.getLowerRow());
    }

    @Test
    void shiftBuildingsDownMovesSurvivingCardsDown() {
        Card building = card("b", Era.I, true, false, false);
        Card event = card("e", Era.I, false, false, true);
        board.getUpperRow().add(building);
        board.getUpperRow().add(event);

        board.shiftBuildingsDown();

        assertEquals(List.of(event), board.getUpperRow());
        assertEquals(List.of(building), board.getLowerRow());
    }

    // ==================== refillUpperRow ====================

    @Test
    void refillUpperRowDrawsCardsNeededWhenDeckIsLarge() {
        board.getTurnOrderTile().setup(2); // 2 slots -> cardsNeeded = 6
        List<Card> deck = deckOfNonEvents(15);

        Era result = board.refillUpperRow(deck, Era.I);

        assertEquals(6, board.getUpperRow().size());
        assertEquals(9, deck.size());
        assertEquals(Era.I, result);
    }

    @Test
    void refillUpperRowDrainsDeckWhenItIsNearlyEmpty() {
        board.getTurnOrderTile().setup(2); // cardsNeeded = 6, threshold 2*6 = 12
        List<Card> deck = deckOfNonEvents(8); // 8 < 12 -> drain everything

        board.refillUpperRow(deck, Era.I);

        assertEquals(8, board.getUpperRow().size());
        assertTrue(deck.isEmpty());
    }

    @Test
    void refillUpperRowReportsAdvancedEraWhenAHigherEraCardSurfaces() {
        board.getTurnOrderTile().setup(2);
        List<Card> deck = new ArrayList<>();
        deck.add(card("era2", Era.II, false, false, false)); // drawn first
        for (int i = 0; i < 5; i++) deck.add(nonEvent("n" + i));

        Era result = board.refillUpperRow(deck, Era.I);

        assertEquals(Era.II, result);
    }

    // ==================== misc ====================

    @Test
    void revealNewBuildingsAppendsToUpperRow() {
        board.getUpperRow().add(nonEvent("existing"));
        Building b1 = new Building("b1", Era.I, 1, 5, 3, null);
        Building b2 = new Building("b2", Era.I, 1, 5, 3, null);

        board.revealNewBuildings(List.of(b1, b2));

        assertEquals(3, board.getUpperRow().size());
        assertTrue(board.getUpperRow().contains(b1));
        assertTrue(board.getUpperRow().contains(b2));
    }

    @Test
    void getTotemsOnOffersCountCountsOccupiedTiles() {
        board.initTrack(2);
        assertEquals(0, board.getTotemsOnOffersCount());

        board.getOfferTrack().get(0).placeTotem(newTotem());
        board.getOfferTrack().get(1).placeTotem(newTotem());

        assertEquals(2, board.getTotemsOnOffersCount());
    }

    // ==================== helpers ====================

    private List<Character> letters() {
        List<Character> result = new ArrayList<>();
        for (OfferTile t : board.getOfferTrack()) result.add(t.getLetter());
        return result;
    }

    private OfferTile tile(char letter) {
        return board.getOfferTrack().stream()
                .filter(t -> t.getLetter() == letter)
                .findFirst().orElseThrow();
    }

    private Totem newTotem() {
        Player p = new Player("p");
        return new Totem("red", p);
    }

    private static Card nonEvent(String id) {
        return card(id, Era.I, false, false, false);
    }

    private static Card event(String id) {
        return card(id, Era.I, false, false, true);
    }

    private static List<Card> deckOfNonEvents(int count) {
        List<Card> deck = new ArrayList<>();
        for (int i = 0; i < count; i++) deck.add(nonEvent("n" + i));
        return deck;
    }

    private static Card card(String id, Era era, boolean survives, boolean destroyedEraIII, boolean event) {
        return new StubCard(id, era, survives, destroyedEraIII, event);
    }

    /** Minimal Card whose round-end flags are fully controllable. */
    private static class StubCard extends Card {
        private final boolean survives;
        private final boolean destroyedEraIII;
        private final boolean event;

        StubCard(String id, Era era, boolean survives, boolean destroyedEraIII, boolean event) {
            super(id, era, 1);
            this.survives = survives;
            this.destroyedEraIII = destroyedEraIII;
            this.event = event;
        }

        @Override public void addToTribe(Player player, Tribe tribe) { }
        @Override public boolean survivesRoundEnd() { return survives; }
        @Override public boolean isDestroyedOnEraIII() { return destroyedEraIII; }
        @Override public boolean isEvent() { return event; }
    }
}
