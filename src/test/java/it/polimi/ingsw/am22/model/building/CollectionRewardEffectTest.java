package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.Inventor;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollectionRewardEffectTest {

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        // Start every test with a fresh player (starts with 0 food)
        testPlayer = new Player("Alice");
    }

    @Test
    void testInventorPairReward() {
        // Constructor: (conditionType, foodReward)
        CollectionRewardEffect effect = new CollectionRewardEffect(CollectionCondition.INVENTOR_PAIR, 3);

        // 1. Add 1st Inventor (Icon 'A') -> 0 pairs total
        Inventor inv1 = new Inventor("INV1", Era.I, 2, 'A');
        testPlayer.getTribe().addCharacter(inv1);
        effect.onCharacterAdded(testPlayer, inv1);

        assertEquals(0, testPlayer.getFood(), "One inventor should yield 0 pairs and 0 food");

        // 2. Add 2nd Inventor (Icon 'A') -> 1 pair total -> +3 Food!
        Inventor inv2 = new Inventor("INV2", Era.I, 2, 'A');
        testPlayer.getTribe().addCharacter(inv2);
        effect.onCharacterAdded(testPlayer, inv2);

        assertEquals(3, testPlayer.getFood(), "Completing a pair should yield 3 food");

        // 3. Add 3rd Inventor (Icon 'A') -> Still only 1 complete pair
        Inventor inv3 = new Inventor("INV3", Era.I, 2, 'A');
        testPlayer.getTribe().addCharacter(inv3);
        effect.onCharacterAdded(testPlayer, inv3);

        assertEquals(3, testPlayer.getFood(), "A third matching inventor does not make a new pair");

        // 4. Add 4th Inventor (Icon 'B') -> Different icon, so still 1 pair
        Inventor inv4 = new Inventor("INV4", Era.I, 2, 'B');
        testPlayer.getTribe().addCharacter(inv4);
        effect.onCharacterAdded(testPlayer, inv4);

        assertEquals(3, testPlayer.getFood(), "A new inventor with a different icon does not make a pair");

        // 5. Add 5th Inventor (Icon 'B') -> Completes the 'B' pair! -> +3 Food!
        Inventor inv5 = new Inventor("INV5", Era.I, 2, 'B');
        testPlayer.getTribe().addCharacter(inv5);
        effect.onCharacterAdded(testPlayer, inv5);

        assertEquals(6, testPlayer.getFood(), "Completing a second unique pair should yield another 3 food");

        // Add a random non-inventor to prove the loop ignores it safely
        testPlayer.getTribe().addCharacter(new TribeCharacter("DUMMY", Era.I, 2, CharacterType.BUILDER, null));
        effect.onCharacterAdded(testPlayer, inv5); // Trigger effect again

        assertEquals(6, testPlayer.getFood(), "Non-inventors should be ignored entirely");
    }

    @Test
    void testSetOf6Reward() {
        // Reward is 5 food per complete set
        CollectionRewardEffect effect = new CollectionRewardEffect(CollectionCondition.SET_OF_6, 5);
        CharacterType[] allTypes = CharacterType.values();

        // 1. Add one of every character type EXCEPT the very last one
        for (int i = 0; i < allTypes.length - 1; i++) {
            TribeCharacter c = new TribeCharacter("DUMMY_" + i, Era.I, 2, allTypes[i],null);
            testPlayer.getTribe().addCharacter(c);
            effect.onCharacterAdded(testPlayer, c);
        }

        // We are missing one character, so we have 0 complete sets
        assertEquals(0, testPlayer.getFood(), "An incomplete set should yield 0 food");

        // 2. Add the final missing character type!
        TribeCharacter finalChar = new TribeCharacter("FINAL", Era.I, 2, allTypes[allTypes.length - 1],null);
        testPlayer.getTribe().addCharacter(finalChar);
        effect.onCharacterAdded(testPlayer, finalChar);

        // We now have 1 complete set -> +5 Food!
        assertEquals(5, testPlayer.getFood(), "Completing the set should grant 5 food");

        // 3. Add an extra Builder (the bottleneck is still 1 for the other types)
        TribeCharacter extraChar = new TribeCharacter("EXTRA", Era.I, 2, CharacterType.BUILDER,null);
        testPlayer.getTribe().addCharacter(extraChar);
        effect.onCharacterAdded(testPlayer, extraChar);

        // Food should remain 5 because we didn't complete a SECOND full set
        assertEquals(5, testPlayer.getFood(), "Adding duplicate characters does not complete a new set");
    }

    @Test
    void testUnknownCondition() {
        // Create an effect with a null condition
        CollectionRewardEffect effect = new CollectionRewardEffect(null, 5);

        // Add a character
        TribeCharacter c = new TribeCharacter("DUMMY", Era.I, 2, CharacterType.BUILDER, null);
        testPlayer.getTribe().addCharacter(c);
        effect.onCharacterAdded(testPlayer, c);

        // Prove nothing crashed and no food was given
        assertEquals(0, testPlayer.getFood(), "An unknown condition should yield 0 food");
    }
}
