package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.Builder;
import it.polimi.ingsw.am22.model.character.Collector;
import it.polimi.ingsw.am22.model.character.Hunter;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EndGameScoringEffectTest {

    private Tribe testTribe;

    @BeforeEach
    void setUp() {
        // Start every test with a clean, empty tribe
        testTribe = new Tribe();
    }

    @Test
    void testFlatPP() {
        EndGameScoringEffect effect = new EndGameScoringEffect(25, 0, null, 0, false);

        // Even with an empty tribe, a flat 25 PP card should return exactly 25
        assertEquals(25, effect.calculateEndGame(testTribe), "Flat PP should be awarded regardless of tribe contents");
    }

    @Test
    void testMultiplierPP() {
        EndGameScoringEffect effect = new EndGameScoringEffect(0, 0, CharacterType.HUNTER, 3, false);

        // Using your exact Hunter constructor: (id, era, minPlayers, stringType, hasFoodIcon)
        testTribe.addCharacter(new Hunter("HUNT1", Era.I, 2, false));
        testTribe.addCharacter(new Hunter("HUNT2", Era.I, 2, true));

        // Using your exact Collector constructor to ensure it doesn't cross-count
        testTribe.addCharacter(new Collector("COL1", Era.I, 2));

        // 2 Hunters * 3 PP = 6 PP total
        assertEquals(6, effect.calculateEndGame(testTribe), "Multiplier should only count the target character type");
    }

    @Test
    void testZeroMultiplierEdgeCase() {
        // Here we set a valid target (HUNTER) but a multiplier of exactly 0
        EndGameScoringEffect effect = new EndGameScoringEffect(0, 0, CharacterType.HUNTER, 0, false);

        // Add a Hunter to the tribe using your exact Hunter constructor
        testTribe.addCharacter(new Hunter("HUNT1", Era.I, 2, false));

        // The logic should see the 0 multiplier and award 0 extra points
        assertEquals(0, effect.calculateEndGame(testTribe), "A zero multiplier should yield 0 points, even if the character exists");
    }

    @Test
    void testDoubleBuilderPP() {
        EndGameScoringEffect effect = new EndGameScoringEffect(0, 0, null, 0, true);

        // Using your exact Builder constructor: (id, era, minPlayers, stringType, discountFood, PP)
        testTribe.addCharacter(new Builder("BLD1", Era.I, 2, 1, 4)); // 4 PP
        testTribe.addCharacter(new Builder("BLD2", Era.I, 2, 0, 3)); // 3 PP

        // Adding an Artist using the base class (since you didn't provide an Artist class)
        testTribe.addCharacter(new TribeCharacter("ART1", Era.I, 2, CharacterType.ARTIST,null));

        // Expected extra points: 4 + 3 = 7
        assertEquals(7, effect.calculateEndGame(testTribe), "Double Builder effect should sum the base PP of only Builder cards");
    }

    @Test
    void testSetsOf6() {
        EndGameScoringEffect effect = new EndGameScoringEffect(0, 5, null, 0, false);

        assertEquals(0, effect.calculateEndGame(testTribe), "Empty tribe should yield 0 sets");

        // Add exactly ONE of every single character type using the parent class
        // to quickly build a complete set of 6
        for (CharacterType type : CharacterType.values()) {
            testTribe.addCharacter(new TribeCharacter("DUMMY", Era.I, 2, type,null));
        }

        // We now have exactly 1 complete set. 1 * 5 PP = 5
        assertEquals(5, effect.calculateEndGame(testTribe), "One complete set should yield 5 PP");

        // Add a second copy of JUST the Builder.
        testTribe.addCharacter(new Builder("DUMMY_B", Era.I, 2, 0, 2));
        assertEquals(5, effect.calculateEndGame(testTribe), "Incomplete second set should not grant extra points");
    }

    @Test
    void testCombinedEffects() {
        EndGameScoringEffect effect = new EndGameScoringEffect(5, 0, CharacterType.ARTIST, 2, true);

        testTribe.addCharacter(new Builder("BLD1", Era.I, 2, 1, 4)); // Extra Builder PP: 4
        testTribe.addCharacter(new TribeCharacter("ART1", Era.I, 2, CharacterType.ARTIST,null)); // Artist Multiplier: 2

        // Total expected: 5 (Flat) + 4 (Builder) + 2 (Artist) = 11
        assertEquals(11, effect.calculateEndGame(testTribe), "Combined effects should sum together perfectly");
    }
}
