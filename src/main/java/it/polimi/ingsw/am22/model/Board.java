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

        // Ordiniamo le tessere offerta in ordine alfabetico crescente per lettera
        offerTrack.sort(Comparator.comparingInt(OfferTile::getLetter));
    }
    public void dealInitialCards(List<Card> tribeDeck, int numPlayers) {
        // Fila inferiore = numero giocatori + 1.
        // Se si pesca una carta Evento, va spostata nella fila superiore
        // e si continua a pescare fino a completare la fila inferiore.
        int lowerTarget = numPlayers + 1;
        while (lowerRow.size() < lowerTarget && !tribeDeck.isEmpty()) {
            Card drawn = tribeDeck.removeFirst();
            if (drawn.isEvent()) {
                upperRow.add(drawn);
            } else {
                lowerRow.add(drawn);
            }
        }

        // Regola del manuale (passo 5): Fila superiore = numero giocatori + 4.
        // Se ci sono già Event spostati dal passo precedente, si pesca solo
        // il numero di carte necessario per completare la fila.
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

