package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * The shared game board: the two rows of cards on offer (upper and lower), the
 * {@link TurnOrderTile}, and the offer track of {@link OfferTile}s. It owns the
 * card-flow logic for the opening deal, round-end maintenance and refilling.
 */
public class Board implements Serializable {
    private List<Card> upperRow;
    private List<Card> lowerRow;
    private TurnOrderTile turnOrderTile;
    private List<OfferTile> offerTrack;

    /**
     * @param numPlayers the number of players (the rows and tracks are populated
     *                   later by {@link #initTrack(int)} and {@link #dealInitialCards(List, int)})
     */
    public Board(int numPlayers) {
        upperRow = new ArrayList<>();
        lowerRow = new ArrayList<>();
        turnOrderTile = new TurnOrderTile();
        offerTrack = new ArrayList<>();
    }

    /**
     * (Re)builds the offer track for the given number of players and sorts the
     * tiles by letter. Extra tiles are unlocked for 3, 4 and 5 players.
     *
     * @param numPlayers the number of players
     */
    public void initTrack(int numPlayers) {
        offerTrack.clear();
        offerTrack.add(new OfferTile('B', 0, 1, 0));
        offerTrack.add(new OfferTile('C', 1, 0, 0));
        offerTrack.add(new OfferTile('E', 1, 1, 0));
        offerTrack.add(new OfferTile('F', 2, 0, 0));
        if (numPlayers >= 3) offerTrack.add(new OfferTile('D', 0, 2, 0));
        if (numPlayers >= 4) offerTrack.add(new OfferTile('G', 2, 1, 0));
        if (numPlayers == 5) offerTrack.add(new OfferTile('A', 0, 0, 3));

        offerTrack.sort(Comparator.comparingInt(OfferTile::getLetter));
    }

    /**
     * Deals the opening rows. The lower row receives {@code numPlayers + 1}
     * non-Event cards; any Event drawn while filling it is diverted to the upper
     * row. The upper row is then topped up to {@code numPlayers + 4} cards
     * (counting any Event already moved there).
     *
     * @param tribeDeck  the deck to draw from (mutated)
     * @param numPlayers the number of players
     */
    public void dealInitialCards(List<Card> tribeDeck, int numPlayers) {
        // Lower row gets (numPlayers + 1) non-Event cards. Events drawn into this row
        // are diverted to the upper row and the lower row keeps drawing.
        int lowerTarget = numPlayers + 1;
        while (lowerRow.size() < lowerTarget && !tribeDeck.isEmpty()) {
            Card drawn = tribeDeck.removeFirst();
            if (drawn.isEvent()) upperRow.add(drawn);
            else lowerRow.add(drawn);
        }

        // Then top up the upper row to (numPlayers + 4), counting any Event already moved there.
        int upperTarget = numPlayers + 4;
        while (upperRow.size() < upperTarget && !tribeDeck.isEmpty()) {
            upperRow.add(tribeDeck.removeFirst());
        }
    }

    /** Removes from the lower row the cards that do not survive the round end. */
    public void clearLowerRow() {
        lowerRow.removeIf(card -> !card.survivesRoundEnd());
    }

    /** Removes from the lower row the cards (buildings) destroyed at the start of Era III. */
    public void clearLowerBuildings() {
        lowerRow.removeIf(Card::isDestroyedOnEraIII);
    }

    /** Moves the non-surviving cards from the upper row down to the lower row. */
    public void shiftUpToLow() {
        moveFromUpperToLower(card -> !card.survivesRoundEnd());
    }

    /** Moves the surviving cards (buildings) from the upper row down to the lower row. */
    public void shiftBuildingsDown() {
        moveFromUpperToLower(Card::survivesRoundEnd);
    }

    // Moves every upper-row card matching the predicate to the lower row.
    private void moveFromUpperToLower(Predicate<Card> condition) {
        List<Card> moved = new ArrayList<>();
        upperRow.removeIf(card -> {
            if (condition.test(card)) {
                moved.add(card);
                return true;//card removed
            }
            return false;
        });
        lowerRow.addAll(moved);
    }

    /**
     * Refills the upper row from the deck after a round. Normally draws enough
     * cards to refill; but if fewer than that many cards would remain afterwards,
     * the whole deck is drained instead, so the two Final Event cards at the
     * bottom always surface before the last round.
     *
     * @param tribeDeck  the deck to draw from (mutated)
     * @param currentEra the current Era
     * @return the (possibly advanced) Era implied by the cards drawn
     */
    public Era refillUpperRow(List<Card> tribeDeck, Era currentEra) {
        int cardsNeeded = turnOrderTile.getSlots().size() + 4;
        Era newEra = currentEra;

        // Normally draw cardsNeeded. But if after that the deck would have fewer than
        // cardsNeeded cards left, drain the whole deck instead, so the two Final Event
        // cards at the bottom always surface before the last round.
        int drawCount = tribeDeck.size() < 2 * cardsNeeded ? tribeDeck.size() : cardsNeeded;
        for (int i = 0; i < drawCount; i++) {
            Card drawn = tribeDeck.removeFirst();
            upperRow.add(drawn);
            if (drawn.getEra().ordinal() > newEra.ordinal()) {
                newEra = drawn.getEra();
            }
        }
        return newEra;
    }

    /**
     * Reveals new buildings by appending them to the upper row.
     *
     * @param newBuildings the buildings to reveal
     */
    public void revealNewBuildings(List<Building> newBuildings) {
        upperRow.addAll(newBuildings);
    }

    /** @return the upper row of cards */
    public List<Card> getUpperRow() { return upperRow; }

    /** @return the lower row of cards */
    public List<Card> getLowerRow() { return lowerRow; }

    /** @return the turn-order tile */
    public TurnOrderTile getTurnOrderTile() { return turnOrderTile; }

    /** @return the offer track */
    public List<OfferTile> getOfferTrack() { return offerTrack; }

    /** @return how many offer tiles are currently occupied by a totem */
    public int getTotemsOnOffersCount() {
        int count = 0;
        for (OfferTile tile : offerTrack) {
            if (!tile.isAvailable()) count++;
        }
        return count;
    }
}
