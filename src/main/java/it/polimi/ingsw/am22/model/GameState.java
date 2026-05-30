package it.polimi.ingsw.am22.model;

import java.io.Serializable;
import java.util.List;

public interface GameState extends Serializable {

    // If a state does not override the method, the exception fires automatically.
    default void startMatch(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default void placeTotemOnOffer(Game game, Player player, OfferTile tile) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default void pickCards(Game game, Player player, List<Card> selectedCards) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default void pickBonusCard(Game game, Player player, Card bonusCard) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default void resolveEvents(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default void updateRound(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    default Player determineWinner(Game game) {
        throw new IllegalStateException("Action not allowed in: " + getPhaseName());
    }
    String getPhaseName();

    /** True only for the initial setup phase. */
    default boolean isSetupPhase() { return false; }

    /** True only for the end-game phase. */
    default boolean isEndGame() { return false; }
}