package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.Locale;

public abstract class Card implements Serializable {
    private final String id;
    private final Era era;
    private final int minPlayers;

    public Card(String id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public abstract void addToTribe(Player player, Tribe tribe);

    /** Pre-flight check before any state mutation. Unpickable cards (e.g. Events) override and throw. */
    public void validatePickable() {}

    /** Non-throwing counterpart of {@link #validatePickable()}, used to count pickable cards in a row. */
    public boolean isPickable() { return true; }

    /** Optional purchases (Buildings) are never forced just to satisfy a tile's count requirement. */
    public boolean isOptionalPurchase() { return false; }

    public int getFoodCost() { return 0; }

    /**
     * Validation phase: declare how being picked at this point in the sequence affects
     * the simulated food / builder discount / hunter count. Default: no effect.
     */
    public void applyPickEffect(PickSimulation sim) {}

    /** Commit phase: pay food cost on the real player. Default: free. */
    public void payPickCost(Player player) {}

    public void onRoundEndTrigger(Game game) {}

    /** Resolution priority for end-of-round triggers. 1 means "deferred" (Sustenance). */
    public int getTriggerPriority() { return 0; }

    /** Whether the card stays on the board at round end. Characters/Events are discarded by default. */
    public boolean survivesRoundEnd() { return false; }

    public boolean isDestroyedOnEraIII() { return false; }

    public boolean isEvent() { return false; }

    /** Number of Shaman stars on this card. Only Shaman overrides; everything else is 0. */
    public int getNumStars() { return 0; }

    /** Card macro-category for the network DTO ("CHARACTER", "BUILDING", "EVENT"). */
    public String cardCategory() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    /** Specific type within the category (e.g. CharacterType, EventType). */
    public String cardDetailType() { return getClass().getSimpleName().toUpperCase(Locale.ROOT); }

    /**
     * Human-readable explanation of what this card does, shown by the TUI
     * {@code check} command. Subclasses override to expose the strategic
     * effect text without leaking model internals.
     */
    public String describe() { return cardDetailType(); }

    public String getId() {return id;}
    public Era getEra() {return era;}
    public int getMinPlayers() {return minPlayers;}
}