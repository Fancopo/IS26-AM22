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

    /**
     * @param initialFood            the player's current food
     * @param initialBuilderDiscount the tribe's current Builder discount
     * @param initialHunterCount     the number of Hunters currently in the tribe
     */
    public PickSimulation(int initialFood, int initialBuilderDiscount, int initialHunterCount) {
        this.food = initialFood;
        this.builderDiscount = initialBuilderDiscount;
        this.hunterCount = initialHunterCount;
    }

    /** @return the simulated food currently available */
    public int getFood() { return food; }

    /** @return the simulated Builder discount currently in effect */
    public int getBuilderDiscount() { return builderDiscount; }

    /** @return the simulated number of Hunters currently in the tribe */
    public int getHunterCount() { return hunterCount; }

    /**
     * Pays the given food cost.
     *
     * @param cost the food to subtract
     * @throws IllegalStateException if the available food is less than {@code cost}
     */
    public void payFood(int cost) {
        if (food < cost) {
            throw new IllegalStateException("Insufficient food to purchase the selected cards.");
        }
        food -= cost;
    }

    /**
     * Adds food to the simulation (e.g. a Hunter feeding the tribe).
     *
     * @param amount the food to add
     */
    public void addFood(int amount) { food += amount; }

    /**
     * Increases the simulated Builder discount (e.g. a freshly picked Builder).
     *
     * @param amount the extra discount
     */
    public void addBuilderDiscount(int amount) { builderDiscount += amount; }

    /** Records that one more Hunter has joined the simulated tribe. */
    public void incrementHunterCount() { hunterCount++; }
}
