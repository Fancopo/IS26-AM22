package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.Building.Building;

import java.util.ArrayList;
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
        // Svuotiamo la lista nel caso venga richiamata per un riavvio
        offerTrack.clear();

        // Formare il tracciato accostando le tessere in ordine alfabetico
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
    }
    public void dealInitialCards(List<Card> tribeDeck, int numPlayers) {
        // Regola del manuale: Fila inferiore = numero giocatori + 1
        for (int i = 0; i < numPlayers + 1; i++) {
            if (!tribeDeck.isEmpty()) {
                lowerRow.add(tribeDeck.remove(0));
            }
        }

        // Regola del manuale: Fila superiore = numero giocatori + 4
        for (int i = 0; i < numPlayers + 4; i++) {
            if (!tribeDeck.isEmpty()) {
                upperRow.add(tribeDeck.remove(0));
            }
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
        for (int i = 0; i < cardsNeeded; i++) {
            if (!tribeDeck.isEmpty()) {
                upperRow.add(tribeDeck.removeFirst());
            }
        }

        if (!upperRow.isEmpty() && upperRow.getFirst().getEra() != currentEra) {
            return upperRow.getFirst().getEra();
        }
        return currentEra;
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
