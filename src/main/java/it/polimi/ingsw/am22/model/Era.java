package it.polimi.ingsw.am22.model;

/**
 * The three historical ages a card (and the game as a whole) can belong to.
 * Cards surface in Era order during the game, and the current Era advances as
 * higher-Era cards are drawn onto the board.
 */
public enum Era {
    /** First age: the cards in play at the start of the game. */
    I,
    /** Second age. */
    II,
    /** Third and final age. */
    III
}
