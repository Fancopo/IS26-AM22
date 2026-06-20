package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.List;

/**
 * A phase of the game, modelled with the State pattern. Each concrete state
 * implements only the actions that are legal while it is active; every other
 * action inherits the default implementation below, which rejects the call with
 * an {@link IllegalStateException}.
 *
 * @see it.polimi.ingsw.am22.model.states
 */
public interface GameState extends Serializable {

    // If a state does not override the method, the exception fires automatically.

    /**
     * Starts the match (deck setup, initial deal, turn order).
     *
     * @param game the game being driven
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void startMatch(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Places the given player's totem on an offer tile.
     *
     * @param game   the game being driven
     * @param player the player acting
     * @param tile   the chosen offer tile
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void placeTotemOnOffer(Game game, Player player, OfferTile tile) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Resolves a player's card pick for the offer tile their totem sits on.
     *
     * @param game          the game being driven
     * @param player        the player acting
     * @param selectedCards the cards the player chose (possibly empty)
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void pickCards(Game game, Player player, List<Card> selectedCards) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Resolves the optional end-of-round bonus pick.
     *
     * @param game      the game being driven
     * @param player    the player entitled to the bonus pick
     * @param bonusCard the card chosen from the upper row
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void pickBonusCard(Game game, Player player, Card bonusCard) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Triggers every end-of-round Event currently in play.
     *
     * @param game the game being driven
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void resolveEvents(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Performs end-of-round cleanup, board refill and turn-order update.
     *
     * @param game the game being driven
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default void updateRound(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * Computes the winner once the game is over.
     *
     * @param game the finished game
     * @return the winning player
     * @throws IllegalStateException if the action is not allowed in this phase
     */
    default Player determineWinner(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }

    /**
     * @return the human-readable name of this phase
     */
    String getPhaseName();

    /** @return {@code true} only for the initial setup phase */
    default boolean isSetupPhase() { return false; }

    /** @return {@code true} only for the end-game phase */
    default boolean isEndGame() { return false; }
}
