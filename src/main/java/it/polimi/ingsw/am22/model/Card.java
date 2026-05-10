package it.polimi.ingsw.am22.model;

import java.util.Locale;

public abstract class Card {
    private String id;
    private Era era;
    private int minPlayers;

    public Card(String id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public abstract void addToTribe(Player player, Tribe tribe);

    /**
     * Pre-flight check invoked before mutating any game state during a pick action.
     * Subclasses that cannot legally be taken into a player's tribe must override this
     * method and throw, so that an invalid selection is rejected as a whole and no
     * partial mutation (such as adding a sibling card to the tribe) occurs.
     * Default behavior: cards are pickable.
     */
    public void validatePickable() {}

    /**
     * Whether this card can be taken into a tribe. Mirrors {@link #validatePickable()}
     * but as a non-throwing predicate, so the action-resolution logic can count how
     * many pickable cards are available in a row (e.g. when an Event blocks a slot,
     * a tile requiring N cards must be satisfiable with fewer if fewer are pickable).
     */
    public boolean isPickable() { return true; }

    /**
     * Whether picking this card is an optional purchase rather than a mandatory pickup.
     * Buildings cost food and the player chooses whether to buy them, so they should
     * never be forced into a selection just to satisfy a tile's card-count requirement.
     * Characters are mandatory pickups by default.
     */
    public boolean isOptionalPurchase() { return false; }

    public int getFoodCost() {return 0;} // By default, cards have no food cost

    /**
     * Apply this card's effect to the pick simulation during the VALIDATION
     * phase of an action. Subclasses override to declare how being picked
     * (in this position of the sequence) changes the player's simulated
     * food / builder discount / hunter count. A Building deducts food
     * (throwing if insufficient), a Builder increases the discount, a Hunter
     * grows the simulated hunter count and possibly feeds the player.
     * Default: no effect.
     */
    public void applyPickEffect(PickSimulation sim) {}

    /**
     * Perform the food side-effect of being picked, on the real player, during
     * the COMMIT phase. Only cards that cost food (Building) override this;
     * the addition of the card to the tribe is handled separately and
     * polymorphically by {@link #addToTribe(Player, Tribe)}.
     * Default: no cost.
     */
    public void payPickCost(Player player) {}

    public void onRoundEndTrigger(Game game) {} // No default behavior

    public int getTriggerPriority() {return 0;} // Determines resolution order (0 = normal, 1 = deferred).
    // Used so that Sustenance triggers last.

    public boolean survivesRoundEnd() {return false;}  // Whether the card stays on the board at round end.
    // By default, cards (Characters, Events) are discarded.

    public boolean isDestroyedOnEraIII() {return false;} // Whether the card is destroyed when transitioning to Era III.

    /** Defines whether the card is an Event. Used during setup to route Events to the upper row. */
    public boolean isEvent() {return false;}


    /** Card macro-category for the network DTO (e.g. "CHARACTER", "BUILDING", "EVENT"). */
    public String cardCategory() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    /** Specific type within the category (e.g. CharacterType, EventType, "BUILDING"). */
    public String cardDetailType() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    public String getId() {return id;}
    public Era getEra() {return era;}
    public int getMinPlayers() {return minPlayers;}
}