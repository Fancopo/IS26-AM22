package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Board {
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
        // Clear the list in case this is called on restart
        offerTrack.clear();

        // Build the track placing tiles in alphabetical order
        offerTrack.add(new OfferTile('B',0,1,0));
        offerTrack.add(new OfferTile('C',1,0,0));
        offerTrack.add(new OfferTile('E',1,1,0));
        offerTrack.add(new OfferTile('F',2,0,0));

        if (numPlayers >= 3) {
            offerTrack.add(new OfferTile('D',0,2,0));
        }
        if (numPlayers >= 4) {
            offerTrack.add(new OfferTile('G',2,1,0));
        }
        if (numPlayers == 5) {
            offerTrack.add(new OfferTile('A',0,0,3));
        }

        // Sort offer tiles in ascending alphabetical order by letter
        offerTrack.sort(Comparator.comparingInt(OfferTile::getLetter));
    }
    public void dealInitialCards(List<Card> tribeDeck, int numPlayers) {
        // Lower row = number of players + 1.
        // If an Event card is drawn, it is moved to the upper row
        // and we keep drawing until the lower row is filled.
        int lowerTarget = numPlayers + 1;
        while (lowerRow.size() < lowerTarget && !tribeDeck.isEmpty()) {
            Card drawn = tribeDeck.removeFirst();
            if (drawn.isEvent()) {
                upperRow.add(drawn);
            } else {
                lowerRow.add(drawn);
            }
        }

        // Rulebook (step 5): Upper row = number of players + 4.
        // If Events were already moved here in the previous step, only
        // draw the cards needed to fill the row.
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
        List<Card> cardsToMoveDown = new ArrayList<>();

        upperRow.removeIf(card -> {
            if (!card.survivesRoundEnd()) {
                cardsToMoveDown.add(card);
                return true;
            }
            return false;
        });

        lowerRow.addAll(cardsToMoveDown);
    }

    public void shiftBuildingsDown() {
        List<Card> cardsToMoveDown = new ArrayList<>();

        upperRow.removeIf(card -> {
            if (card.survivesRoundEnd()) {
                cardsToMoveDown.add(card);
                return true;
            }
            return false;
        });

        lowerRow.addAll(cardsToMoveDown);
    }

    public Era refillUpperRow(List<Card> tribeDeck, Era currentEra) {
        int cardsNeeded = turnOrderTile.getSlots().size() + 4;
        Era newEra = currentEra;
        for (int i = 0; i < cardsNeeded; i++) {
            if (!tribeDeck.isEmpty()) {
                Card drawn = tribeDeck.removeFirst();
                upperRow.add(drawn);
                if (drawn.getEra().ordinal() > newEra.ordinal()) {
                    newEra = drawn.getEra();
                }
            }
        }
        // If fewer than cardsNeeded remain in the deck, this is the last refill
        // of the game: drain the leftover so the two Final Event cards at the
        // bottom of the deck always end up visible in the upper row before the
        // last round, regardless of how many cards the initial deal consumed.
        if (tribeDeck.size() < cardsNeeded) {
            while (!tribeDeck.isEmpty()) {
                Card drawn = tribeDeck.removeFirst();
                upperRow.add(drawn);
                if (drawn.getEra().ordinal() > newEra.ordinal()) {
                    newEra = drawn.getEra();
                }
            }
        }
        return newEra;
    }

    public void revealNewBuildings(List<Building> newBuildings) {
        upperRow.addAll(newBuildings);
    }
    // --- GETTERS ---
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

