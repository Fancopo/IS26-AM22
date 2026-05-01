package it.polimi.ingsw.am22.model.building;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShamanicModifierEffectTest {

    @Test
    void testThreeStarsEffect() {
        // Matches your Deck Generator: BLD_II_02 (3 Shaman Stars)
        // Constructor: (extraIcons, preventPPLoss, doubleWinPP)
        ShamanicModifierEffect effect = new ShamanicModifierEffect(3, false, false);

        assertEquals(3, effect.getExtraShamanIcons(), "Should grant exactly 3 extra icons");
        assertFalse(effect.preventsShamanPPLoss(), "Should not prevent PP loss");
        assertFalse(effect.doublesShamanWinPP(), "Should not double win PP");
    }

    @Test
    void testPreventLossEffect() {
        // Matches your Deck Generator: BLD_I_04 (Shaman: No PP Loss)
        ShamanicModifierEffect effect = new ShamanicModifierEffect(0, true, false);

        assertEquals(0, effect.getExtraShamanIcons(), "Should not grant extra icons");
        assertTrue(effect.preventsShamanPPLoss(), "Should prevent PP loss");
        assertFalse(effect.doublesShamanWinPP(), "Should not double win PP");
    }

    @Test
    void testDoubleWinEffect() {
        // Matches your Deck Generator: BLD_II_01 (Shaman: Double PP)
        ShamanicModifierEffect effect = new ShamanicModifierEffect(0, false, true);

        assertEquals(0, effect.getExtraShamanIcons(), "Should not grant extra icons");
        assertFalse(effect.preventsShamanPPLoss(), "Should not prevent PP loss");
        assertTrue(effect.doublesShamanWinPP(), "Should double win PP");
    }
}
