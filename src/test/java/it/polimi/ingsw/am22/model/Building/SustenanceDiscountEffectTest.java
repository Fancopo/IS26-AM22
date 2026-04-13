package it.polimi.ingsw.am22.model.Building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.Collector;
import it.polimi.ingsw.am22.model.character.Hunter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SustenanceDiscountEffectTest {

    private Tribe testTribe;

    @BeforeEach
    void setUp() {
        // Start every test with a clean, empty tribe
        testTribe = new Tribe();
    }

    @Test
    void testEmptyTribeDiscount() {
        // Constructor: (targetCharacterType)
        SustenanceDiscountEffect effect = new SustenanceDiscountEffect(CharacterType.COLLECTOR);

        // An empty tribe has 0 Collectors, so the discount should be 0
        assertEquals(0, effect.getSustenanceDiscount(testTribe), "Empty tribe should yield 0 discount");
    }

    @Test
    void testTargetCharacterDiscount() {
        SustenanceDiscountEffect effect = new SustenanceDiscountEffect(CharacterType.COLLECTOR);

        // Add 2 Collectors to the tribe
        testTribe.addCharacter(new Collector("COL1", Era.I, 2));
        testTribe.addCharacter(new Collector("COL2", Era.I, 2));

        // The discount should be exactly 2 (1 per Collector)
        assertEquals(2, effect.getSustenanceDiscount(testTribe), "Discount should match the exact number of target characters");
    }

    @Test
    void testIgnoresOtherCharacters() {
        SustenanceDiscountEffect effect = new SustenanceDiscountEffect(CharacterType.COLLECTOR);

        // Add 1 Collector
        testTribe.addCharacter(new Collector("COL1", Era.I, 2));

        // Add 2 Hunters (These should NOT grant a discount)
        testTribe.addCharacter(new Hunter("HUNT1", Era.I, 2, false));
        testTribe.addCharacter(new Hunter("HUNT2", Era.I, 2, false));

        // The discount should still be exactly 1, ignoring the Hunters
        assertEquals(1, effect.getSustenanceDiscount(testTribe), "Effect must ignore characters that do not match the target type");
    }
}