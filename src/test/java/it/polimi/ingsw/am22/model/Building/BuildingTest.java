package it.polimi.ingsw.am22.model.Building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildingTest {

    private Building testBuilding;
    private MockBuildingEffect mockEffect;

    // 1. CREATE A DUMMY EFFECT
    // A simple mock class so we can control what the effect does during testing
    static class MockBuildingEffect implements BuildingEffect {
        boolean totemPlacedCalled = false;
        boolean grantsExtraBuy = false;
        int calculatedEndGamePP = 0;

        @Override
        public void onTotemPlaced(Player owner) {
            totemPlacedCalled = true;
        }

        @Override
        public boolean hasExtraBuyAtRoundEnd() {
            return grantsExtraBuy;
        }

        @Override
        public int calculateEndGame(Tribe tribe) {
            return calculatedEndGamePP;
        }
    }

    @BeforeEach
    void setUp() {
        // This runs before EVERY test to give us a fresh, clean Building
        mockEffect = new MockBuildingEffect();
        testBuilding = new Building("BLD_01", Era.I, 2, 5, 3, mockEffect);
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals("BLD_01", testBuilding.getId());
        assertEquals(Era.I, testBuilding.getEra());
        assertEquals(5, testBuilding.getFoodPrice());
        assertEquals(3, testBuilding.getFinalPP());
        assertEquals(mockEffect, testBuilding.getEffect());
    }

    @Test
    void testSurvivesRoundEndAndEraIII() {
        // Buildings should always survive the round end and be destroyed on Era III
        assertTrue(testBuilding.survivesRoundEnd(), "Buildings should survive round ends");
        assertTrue(testBuilding.isDestroyedOnEraIII(), "Buildings should be destroyed on Era III");
    }

    @Test
    void testAddToTribe() {
        Tribe dummyTribe = new Tribe();
        Player dummyPlayer = new Player("TestPlayer"); // Use your Player constructor

        // When we tell the building to add itself to the tribe...
        testBuilding.addToTribe(dummyPlayer, dummyTribe);

        // ...the tribe should now contain exactly 1 building, and it should be this one!
        assertEquals(1, dummyTribe.getBuildings().size(), "Tribe should have exactly 1 building");
        assertTrue(dummyTribe.getBuildings().contains(testBuilding), "Tribe should contain the test building");
    }

    @Test
    void testApplyOnTotemPlaced() {
        Player dummyPlayer = new Player("TestPlayer");

        // Call the method
        testBuilding.applyOnTotemPlaced(dummyPlayer);

        // Verify that the Building successfully delegated the call to our mock effect
        assertTrue(mockEffect.totemPlacedCalled, "Building should delegate to the effect's onTotemPlaced method");
    }

    @Test
    void testApplyOnTotemPlacedWithNullEffect() {
        // Test defensive programming: Ensure the game doesn't crash if effect is null
        Building nullEffectBuilding = new Building("BLD_02", Era.II, 2, 0, 0, null);
        Player dummyPlayer = new Player("TestPlayer");

        assertDoesNotThrow(() -> nullEffectBuilding.applyOnTotemPlaced(dummyPlayer),
                "Should safely do nothing if the effect is null");
    }

    @Test
    void testGrantsExtraBuyAtRoundEnd() {
        // Case 1: Effect says NO
        mockEffect.grantsExtraBuy = false;
        assertFalse(testBuilding.grantsExtraBuyAtRoundEnd());

        // Case 2: Effect says YES
        mockEffect.grantsExtraBuy = true;
        assertTrue(testBuilding.grantsExtraBuyAtRoundEnd());

        // Case 3: Effect is NULL
        Building nullEffectBuilding = new Building("BLD_02", Era.II, 2, 0, 0, null);
        assertFalse(nullEffectBuilding.grantsExtraBuyAtRoundEnd(), "Null effect should safely return false");
    }

    @Test
    void testApplyOnFoodSlotPlaced() {
        // Since this is an empty method, we just want to ensure it doesn't crash
        Player dummyPlayer = new Player("TestPlayer");

        assertDoesNotThrow(() -> testBuilding.applyOnFoodSlotPlaced(dummyPlayer),
                "Empty method should execute safely without throwing exceptions");
    }

    @Test
    void testFinalBuildingPP() {
        Tribe tribe = new Tribe();
        Player dummyPlayer = new Player("TestPlayer");

        // Create Building 1 (Base: 3 PP, Effect: 2 PP) = 5 Total
        MockBuildingEffect effect1 = new MockBuildingEffect();
        effect1.calculatedEndGamePP = 2;
        Building b1 = new Building("B1", Era.I, 2, 0, 3, effect1);
        b1.addToTribe(dummyPlayer, tribe);

        // Create Building 2 (Base: 5 PP, Effect: 10 PP) = 15 Total
        MockBuildingEffect effect2 = new MockBuildingEffect();
        effect2.calculatedEndGamePP = 10;
        Building b2 = new Building("B2", Era.II, 2, 0, 5, effect2);
        b2.addToTribe(dummyPlayer, tribe);

        // Calculate the static sum (5 + 15 = 20)
        int grandTotal = Building.FinalBuildingPP(tribe);

        assertEquals(20, grandTotal, "FinalBuildingPP should accurately sum base PP and effect PP for all buildings");
    }
}