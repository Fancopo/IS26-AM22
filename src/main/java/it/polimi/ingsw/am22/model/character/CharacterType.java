package it.polimi.ingsw.am22.model.character;

/**
 * The six kinds of tribe character, each with its own scoring / effect behaviour.
 */
public enum CharacterType {
    /** Scores at end of game based on distinct invention icons. */
    INVENTOR,
    /** Scores prestige points in pairs at end of game (Cave Paintings). */
    ARTIST,
    /** Scores food and PP during the Hunting event. */
    HUNTER,
    /** Discounts Buildings and scores its printed PP at end of game. */
    BUILDER,
    /** Contributes star icons for the Shamanic Ritual event. */
    SHAMAN,
    /** Feeds characters for free during the Sustenance event. */
    COLLECTOR
}
