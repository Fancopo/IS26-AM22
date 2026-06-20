package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.Locale;

/**
 * Base type for every card in the game (tribe characters, buildings and events).
 * Behaviour that varies per card kind is exposed as overridable methods with
 * sensible no-op / identity defaults, so callers never need to test for the
 * concrete subtype.
 */
public abstract class Card implements Serializable {
    private final String id;
    private final Era era;
    private final int minPlayers;

    /**
     * @param id         the card's identifier
     * @param era        the Era the card belongs to
     * @param minPlayers the minimum player count for this card to be in play
     */
    public Card(String id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    /**
     * Adds this card to the given tribe; each subtype routes itself to the right
     * collection and applies its on-add side effects.
     *
     * @param player the owner of the tribe
     * @param tribe  the tribe the card joins
     */
    public abstract void addToTribe(Player player, Tribe tribe);

    /** Pre-flight check before any state mutation. Unpickable cards (e.g. Events) override and throw. */
    public void validatePickable() {}

    /**
     * Non-throwing counterpart of {@link #validatePickable()}, used to count pickable cards in a row.
     *
     * @return {@code true} if this card can be picked
     */
    public boolean isPickable() { return true; }

    /**
     * @return {@code true} for optional purchases (Buildings), which are never forced
     *         into a selection just to satisfy a tile's count requirement
     */
    public boolean isOptionalPurchase() { return false; }

    /** @return the food cost of taking this card (0 unless overridden) */
    public int getFoodCost() { return 0; }

    /**
     * Validation phase: declare how being picked at this point in the sequence affects
     * the simulated food / builder discount / hunter count. Default: no effect.
     *
     * @param sim the running pick simulation
     */
    public void applyPickEffect(PickSimulation sim) {}

    /**
     * Commit phase: pay food cost on the real player. Default: free.
     *
     * @param player the player paying for the pick
     */
    public void payPickCost(Player player) {}

    /**
     * Round-end hook: apply this card's end-of-round effect. Default: nothing.
     *
     * @param game the game being resolved
     */
    public void onRoundEndTrigger(Game game) {}

    /** @return resolution priority for end-of-round triggers; 1 means "deferred" (Sustenance) */
    public int getTriggerPriority() { return 0; }

    /** @return whether the card stays on the board at round end; Characters/Events are discarded by default */
    public boolean survivesRoundEnd() { return false; }

    /** @return whether the card is destroyed at the start of Era III */
    public boolean isDestroyedOnEraIII() { return false; }

    /** @return whether this card is an Event */
    public boolean isEvent() { return false; }

    /** @return the number of Shaman stars on this card; only Shaman overrides, everything else is 0 */
    public int getNumStars() { return 0; }

    /** @return the card macro-category for the network DTO ("CHARACTER", "BUILDING", "EVENT") */
    public String cardCategory() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    /** @return the specific type within the category (e.g. CharacterType, EventType) */
    public String cardDetailType() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    /**
     * Human-readable explanation of what this card does, shown by the TUI
     * {@code check} command. Subclasses override to expose the strategic
     * effect text without leaking model internals.
     *
     * @return a description of the card's effect
     */
    public String describe() { return cardDetailType(); }

    /** @return the card's identifier */
    public String getId() {return id;}

    /** @return the Era the card belongs to */
    public Era getEra() {return era;}

    /** @return the minimum player count for this card to be in play */
    public int getMinPlayers() {return minPlayers;}
}
