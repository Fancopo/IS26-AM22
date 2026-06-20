package it.polimi.ingsw.am22.model.event;

/**
 * The four kinds of end-of-round event.
 */
public enum EventType {
    /** Rewards or penalises players based on their Artists. */
    CAVE_PAINTING,
    /** Rewards players based on their Hunters. */
    HUNTING,
    /** Compares star icons across players, rewarding the most and penalising the fewest. */
    SHAMANIC_RITUAL,
    /** Makes players spend food to feed their characters. */
    SUSTENANCE
}
