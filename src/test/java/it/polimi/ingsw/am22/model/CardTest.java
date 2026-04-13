package it.polimi.ingsw.am22.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    private DummyCard testCard;

    // 1. CREATE A DUMMY CLASS
    // We create a basic concrete implementation of the abstract Card class
    // just for testing purposes.
    static class DummyCard extends Card {

        boolean wasAddedToTribe = false;
        Player assignedPlayer = null; // Used to test your new parameter

        public DummyCard(String id, Era era, int minPlayers) {
            super(id, era, minPlayers);
        }

        // We implement the updated abstract method to track both state changes
        @Override
        public void addToTribe(Player player, Tribe tribe) {
            this.wasAddedToTribe = true;
            this.assignedPlayer = player;
        }
    }

    @BeforeEach
    void setUp() {
        // Initialize our dummy card before every test
        testCard = new DummyCard("CARD_001", Era.I, 2);
    }

    @Test
    void testConstructorAndGetters() {
        // Verify that the constructor correctly assigned the variables
        assertEquals("CARD_001", testCard.getId(), "ID should match the constructor input");
        assertEquals(Era.I, testCard.getEra(), "Era should match the constructor input");
        assertEquals(2, testCard.getMinPlayers(), "Min players should match the constructor input");
    }

    @Test
    void testDefaultMethods() {
        // Verify all the default behaviors you programmed into the parent class
        assertEquals(0, testCard.getFoodCost(), "Default food cost should be 0");
        assertEquals(0, testCard.getTriggerPriority(), "Default trigger priority should be 0");
        assertFalse(testCard.survivesRoundEnd(), "By default, cards should not survive the round end");
        assertFalse(testCard.isDestroyedOnEraIII(), "By default, cards should not be destroyed on Era III");
    }

    @Test
    void testOnRoundEndTrigger() {
        // 1. Create a dummy list with at least one player so the Board initializes safely
        List<Player> dummyPlayers = new ArrayList<>();
        dummyPlayers.add(new Player("TestPlayer"));

        // 2. Pass the list into the Game constructor
        Game dummyGame = new Game(dummyPlayers);

        // 3. Run the test
        assertDoesNotThrow(() -> testCard.onRoundEndTrigger(dummyGame),
                "onRoundEndTrigger should execute safely without throwing exceptions");
    }

    @Test
    void testAddToTribe() {
        // Create dummy objects to pass into your new method signature
        Player dummyPlayer = new Player("TestPlayer"); // Assuming Player has a String constructor
        Tribe dummyTribe = new Tribe();

        // Call the method
        testCard.addToTribe(dummyPlayer, dummyTribe);

        // Verify our dummy implementation successfully received the data
        assertTrue(testCard.wasAddedToTribe, "addToTribe should execute the overridden logic");
        assertEquals(dummyPlayer, testCard.assignedPlayer, "The method should receive the correct Player object");
    }
}
