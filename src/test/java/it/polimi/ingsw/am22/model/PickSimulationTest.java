package it.polimi.ingsw.am22.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PickSimulationTest {

    @Test
    void constructorAndGettersExposeInitialValues() {
        PickSimulation sim = new PickSimulation(5, 2, 1);

        assertEquals(5, sim.getFood());
        assertEquals(2, sim.getBuilderDiscount());
        assertEquals(1, sim.getHunterCount());
    }

    @Test
    void addFoodIncreasesAvailableFood() {
        PickSimulation sim = new PickSimulation(5, 0, 0);

        sim.addFood(3);

        assertEquals(8, sim.getFood());
    }

    @Test
    void payFoodDeductsWhenAffordable() {
        PickSimulation sim = new PickSimulation(5, 0, 0);

        sim.payFood(3);

        assertEquals(2, sim.getFood());
    }

    @Test
    void payFoodAllowsSpendingExactlyAllFood() {
        PickSimulation sim = new PickSimulation(5, 0, 0);

        sim.payFood(5);

        assertEquals(0, sim.getFood());
    }

    @Test
    void payFoodThrowsWhenInsufficientAndLeavesFoodUntouched() {
        PickSimulation sim = new PickSimulation(2, 0, 0);

        assertThrows(IllegalStateException.class, () -> sim.payFood(3));
        assertEquals(2, sim.getFood(), "Food must not change when the payment fails");
    }

    @Test
    void addBuilderDiscountAccumulates() {
        PickSimulation sim = new PickSimulation(0, 2, 0);

        sim.addBuilderDiscount(3);

        assertEquals(5, sim.getBuilderDiscount());
    }

    @Test
    void incrementHunterCountAddsOnePerCall() {
        PickSimulation sim = new PickSimulation(0, 0, 1);

        sim.incrementHunterCount();
        sim.incrementHunterCount();

        assertEquals(3, sim.getHunterCount());
    }

    @Test
    void simulatesBuilderThenBuildingSequence() {
        // A Builder picked before a Building lowers what the Building costs:
        // start with 4 food, gain a discount of 2, then pay a 3-cost building
        // for an effective price of 1.
        PickSimulation sim = new PickSimulation(4, 0, 0);

        sim.addBuilderDiscount(2);
        int effectiveCost = Math.max(0, 3 - sim.getBuilderDiscount());
        sim.payFood(effectiveCost);

        assertEquals(3, sim.getFood());
    }
}
