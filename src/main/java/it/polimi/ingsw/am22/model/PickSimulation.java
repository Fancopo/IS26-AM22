package it.polimi.ingsw.am22.model;

/**
 * Mutable scratch state used during the VALIDATION phase of a pick action.
 * Each card in the selected sequence is given a chance to apply its effect on
 * this simulation in order, so an unaffordable selection can be detected
 * before any real game state is mutated.
 *
 * <p>The pick order is semantically meaningful:
 * <ul>
 *   <li>a Builder picked before a Building reduces that Building's cost;</li>
 *   <li>a Hunter* picked before a Building feeds the player so they can
 *       afford it;</li>
 *   <li>a Hunter* picked after another Hunter yields a higher food bonus
 *       (the icon's bonus is "per Hunter currently in the tribe", and the
 *       simulation mirrors that count).</li>
 * </ul>
 */
public class PickSimulation {
    private int food;
    private int builderDiscount;
    private int hunterCount;

    public PickSimulation(int initialFood, int initialBuilderDiscount, int initialHunterCount) {
        this.food = initialFood;
        this.builderDiscount = initialBuilderDiscount;
        this.hunterCount = initialHunterCount;
    }

    public int getFood() { return food; }
    public int getBuilderDiscount() { return builderDiscount; }
    public int getHunterCount() { return hunterCount; }

    /** Pay the given food cost, throwing if insufficient. */
    public void payFood(int cost) {
        if (food < cost) {
            throw new IllegalStateException("Insufficient food to purchase the selected cards.");
        }
        food -= cost;
    }

    public void addFood(int amount) { food += amount; }
    public void addBuilderDiscount(int amount) { builderDiscount += amount; }
    public void incrementHunterCount() { hunterCount++; }
}
