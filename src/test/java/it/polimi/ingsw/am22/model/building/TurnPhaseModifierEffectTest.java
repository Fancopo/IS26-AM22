package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TurnPhaseModifierEffectTest {

    private Player testPlayer;

    @BeforeEach
    void setUp() {
        // Start every test with a fresh player (starts with 0 food)
        testPlayer = new Player("Alice");
    }

    @Test
    void testEffectWithAllBonusesActive() {
        // Constructor: (extraFoodOnTurnOrderBonus, extraBuyAtRoundEnd)
        TurnPhaseModifierEffect effect = new TurnPhaseModifierEffect(true, true);

        // 1. Check extra buy flag
        assertTrue(effect.hasExtraBuyAtRoundEnd(), "Should return true when extra buy flag is active");

        // 2. Check totem placement bonus
        assertEquals(0, testPlayer.getFood(), "Player should start with 0 food");

        effect.onTotemPlaced(testPlayer);

        assertEquals(1, testPlayer.getFood(), "Player should gain exactly 1 food when the totem bonus flag is active");
    }

    @Test
    void testEffectWithNoBonusesActive() {
        // Constructor: (extraFoodOnTurnOrderBonus, extraBuyAtRoundEnd)
        TurnPhaseModifierEffect effect = new TurnPhaseModifierEffect(false, false);

        // 1. Check extra buy flag
        assertFalse(effect.hasExtraBuyAtRoundEnd(), "Should return false when extra buy flag is inactive");

        // 2. Check totem placement bonus
        assertEquals(0, testPlayer.getFood(), "Player should start with 0 food");

        effect.onTotemPlaced(testPlayer);

        assertEquals(0, testPlayer.getFood(), "Player's food should remain exactly 0 when the totem bonus flag is inactive");
    }
}
