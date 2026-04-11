package it.polimi.ingsw.am22;

import java.util.List;

public interface GameState {

    // Se uno stato non sovrascrive il metodo, scatterà in automatico l'eccezione!
    default void startMatch(Game game) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default void placeTotemOnOffer(Game game, Player player, OfferTile tile) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default void pickCards(Game game, Player player, List<Card> selectedCards) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default void pickBonusCard(Game game, Player player, Card bonusCard) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default void resolveEvents(Game game) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default void updateRound(Game game) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    default Player determineWinner(Game game) {
        throw new IllegalStateException("Azione non permessa in: " + getPhaseName());
    }
    String getPhaseName();
}