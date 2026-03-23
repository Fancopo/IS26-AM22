package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Board {
    private List<OfferTile> offerTrack;
    private List<Card> upperRow;
    private List<Card> lowerRow;
    private List<Building> buildingMarket;
    private int numPlayers;
    // Costruttore
    public Board(int numPlayers) {
        this.offerTrack = new ArrayList<>();
        this.upperRow = new ArrayList<>();
        this.lowerRow = new ArrayList<>();
        this.buildingMarket = new ArrayList<>();
        this.numPlayers = numPlayers;
    }


    public void initTrack() {
        // Svuotiamo la lista nel caso venga richiamata per un riavvio
        this.offerTrack.clear();

        // Formare il tracciato accostando le tessere in ordine alfabetico
        this.offerTrack.add(new OfferTile('B',0,1,0));
        this.offerTrack.add(new OfferTile('C',1,0,0));
        this.offerTrack.add(new OfferTile('E',1,1,0));
        this.offerTrack.add(new OfferTile('F',2,0,0));

        if (this.numPlayers >= 3) {
            this.offerTrack.add(new OfferTile('D',0,2,0));
        }
        if (this.numPlayers >= 4) {
            this.offerTrack.add(new OfferTile('G',2,1,0));
        }
        if (this.numPlayers == 5) {
            this.offerTrack.add(new OfferTile('A',0,0,3));
        }
    }

    public Era refillUpperRow(List<Card> deck, Era currentEra) {
        int cardsToDrawUpper = this.numPlayers + 4;
        Era newEra = currentEra;
        int i=0;
        // Continua a pescare finché la fila non è piena o il mazzo finisce
        while (i < cardsToDrawUpper && !deck.isEmpty()) {
            Card drawnCard = deck.remove(0);
            this.upperRow.add(drawnCard);
            i++;
            // "Non appena rivelate una carta dell'Era successiva..."
            if (drawnCard.getEra() != currentEra) {
                newEra = drawnCard.getEra(); // Segna che c'è stato un cambio!
            }
        }
        return newEra; // Restituisce l'informazione al Game
    }
    public void clearLowerBuildings() {
        // Remove cards if they are Buildings
        lowerRow.removeIf(card -> card instanceof Building);
    }
    public void clearLowerRow() {
        // Regola: Scartate tutte le carte Personaggio ed Evento dalla fila inferiore[cite: 212].
        // Gli Edifici rimangono al loro posto[cite: 213].
        this.lowerRow.removeIf(c -> !(c instanceof Building));
    }
    public void shiftUpToLow() {
        // Regola: Spostate nella fila inferiore tutte le carte Personaggio ed Eventi rimaste nella fila superiore
        // Le eventuali carte Edificio rimangono al loro posto
        List<Card> cardsToKeepInUpper = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof Building) {
                cardsToKeepInUpper.add(card);
            } else {
                this.lowerRow.add(card);
            }
        }
        this.upperRow.clear();
        this.upperRow.addAll(cardsToKeepInUpper);
    }

    public void shiftBuildingsDown() {
        List<Card> nonBuildings = new ArrayList<>();
        for (Card card : upperRow) {
            if (card instanceof Building) {
                lowerRow.add(card); // Move building down
            } else {
                nonBuildings.add(card); // Keep other cards
            }
        }
        upperRow.clear();
        upperRow.addAll(nonBuildings);
    }

    public void revealNewBuildings(Era newEra) {
        List<Building> buildingsToAdd = new ArrayList<>();

        // 1. Cerca nel mercato (i mazzetti preparati a inizio partita) gli edifici dell'Era giusta
        for (int i = 0; i < buildingMarket.size(); i++) {
            Building b = buildingMarket.get(i);
            if (b.getEra() == newEra) {
                buildingsToAdd.add(b);
            }
        }

        // 2. Rimuove questi edifici dal mercato (perché ora entrano in gioco)
        buildingMarket.removeAll(buildingsToAdd);

        // 3. Li piazza, scoperti, nella fila superiore
        // Di solito si piazzano a destra delle carte Tribù, quindi li aggiungiamo in coda alla lista
        this.upperRow.addAll(buildingsToAdd);
    }
    // --- GETTERS ---
    public List<OfferTile> getOfferTrack() { return offerTrack; }
    public List<Card> getUpperRow() { return upperRow; }
    public List<Card> getLowerRow() { return lowerRow; }
    public List<Building> getBuildingMarket() { return buildingMarket; }
    public TurnOrderTile getTurnOrderTile() { return turnOrderTile; }
}
