package it.polimi.ingsw.am22.Building;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.event.EventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventYieldBonusEffectTest {

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        // Start every test with a fresh player (0 food, 0 PP)
        testPlayer = new Player("Bob");
    }

    @Test
    void testWrongEventType() {
        // Building gives: +1 Food, +1 PP on HUNTING
        EventYieldBonusEffect effect = new EventYieldBonusEffect(EventType.HUNTING, 1, 1);

        // We trigger CAVE_PAINTING instead with 3 characters
        effect.applyEventBonus(EventType.CAVE_PAINTING, testPlayer, 3);

        // Player should receive nothing because the event types don't match
        assertEquals(0, testPlayer.getFood(), "Food should remain 0 for mismatched event type");
        assertEquals(0, testPlayer.getPP(), "PP should remain 0 for mismatched event type");
    }

    @Test
    void testBothBonusesApplied() {
        // Building gives: +1 Food, +2 PP per character on HUNTING
        EventYieldBonusEffect effect = new EventYieldBonusEffect(EventType.HUNTING, 1, 2);

        // Trigger HUNTING with 3 Hunters
        effect.applyEventBonus(EventType.HUNTING, testPlayer, 3);

        // 3 characters * 1 Food = 3 Food
        assertEquals(3, testPlayer.getFood(), "Player should gain exactly 3 food");
        // 3 characters * 2 PP = 6 PP
        assertEquals(6, testPlayer.getPP(), "Player should gain exactly 6 PP");
    }

    @Test
    void testOnlyFoodBonusApplied() {
        // Building gives: +2 Food, 0 PP per character on CAVE_PAINTING
        EventYieldBonusEffect effect = new EventYieldBonusEffect(EventType.CAVE_PAINTING, 2, 0);

        // Trigger CAVE_PAINTING with 2 Artists
        effect.applyEventBonus(EventType.CAVE_PAINTING, testPlayer, 2);

        // 2 characters * 2 Food = 4 Food
        assertEquals(4, testPlayer.getFood(), "Player should gain 4 food");
        assertEquals(0, testPlayer.getPP(), "PP should remain 0 because the building grants no PP");
    }

    @Test
    void testOnlyPPBonusApplied() {
        // Building gives: 0 Food, +3 PP per character on HUNTING
        EventYieldBonusEffect effect = new EventYieldBonusEffect(EventType.HUNTING, 0, 3);

        // Trigger HUNTING with 2 Hunters
        effect.applyEventBonus(EventType.HUNTING, testPlayer, 2);

        assertEquals(0, testPlayer.getFood(), "Food should remain 0 because the building grants no food");
        // 2 characters * 3 PP = 6 PP
        assertEquals(6, testPlayer.getPP(), "Player should gain 6 PP");
    }

    @Test
    void testZeroCharacterCount() {
        // Building gives: +2 Food, +2 PP on HUNTING
        EventYieldBonusEffect effect = new EventYieldBonusEffect(EventType.HUNTING, 2, 2);

        // Trigger HUNTING, but the player has 0 Hunters
        effect.applyEventBonus(EventType.HUNTING, testPlayer, 0);

        // 0 characters * 2 = 0
        assertEquals(0, testPlayer.getFood(), "Player with 0 characters should gain 0 food");
        assertEquals(0, testPlayer.getPP(), "Player with 0 characters should gain 0 PP");
    }
}