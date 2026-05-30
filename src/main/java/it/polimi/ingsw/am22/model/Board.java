package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class Board implements Serializable {
    private List<Card> upperRow;
    private List<Card> lowerRow;
    private TurnOrderTile turnOrderTile;
    private List<OfferTile> offerTrack;

    public Board(int numPlayers) {
        upperRow = new ArrayList<>();
        lowerRow = new ArrayList<>();
        turnOrderTile = new TurnOrderTile();
        offerTrack = new ArrayList<>();
    }

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

    public void clearLowerRow() {
        lowerRow.removeIf(card -> !card.survivesRoundEnd());
    }

    public void clearLowerBuildings() {
        lowerRow.removeIf(Card::isDestroyedOnEraIII);
    }

    public void shiftUpToLow() {
        moveFromUpperToLower(card -> !card.survivesRoundEnd());
    }

    public void shiftBuildingsDown() {
        moveFromUpperToLower(Card::survivesRoundEnd);
    }

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

    public void revealNewBuildings(List<Building> newBuildings) {
        upperRow.addAll(newBuildings);
    }

    public List<Card> getUpperRow() { return upperRow; }
    public List<Card> getLowerRow() { return lowerRow; }
    public TurnOrderTile getTurnOrderTile() { return turnOrderTile; }
    public List<OfferTile> getOfferTrack() { return offerTrack; }

    public int getTotemsOnOffersCount() {
        int count = 0;
        for (OfferTile tile : offerTrack) {
            if (!tile.isAvailable()) count++;
        }
        return count;
    }
}

