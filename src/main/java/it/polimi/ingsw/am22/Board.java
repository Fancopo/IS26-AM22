package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Board {
    private List<Character> offerTrack;
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

    /**
     * Inizializza il tracciato delle offerte.
     */
    public void initTrack() {
        // Svuotiamo la lista nel caso venga richiamata per un riavvio
        this.offerTrack.clear();

        // Formare il tracciato accostando le tessere in ordine alfabetico
        this.offerTrack.add('B');
        this.offerTrack.add('C');
        this.offerTrack.add('E');
        this.offerTrack.add('F');

        if (this.numPlayers >= 3) {
            this.offerTrack.add('D');
        }
        if (this.numPlayers >= 4) {
            this.offerTrack.add('G');
        }
        if (this.numPlayers == 5) {
            this.offerTrack.add('A');
        }
    }

    /*
     * Ricarica le carte sul tabellone pescando dal mazzo.
     * Viene usato sia per il setup iniziale che alla fine del round.
     */
    public void refill(List<Card> deck) {
        // Regola Fine Round: Pescate dal mazzo un numero di carte pari al
        // numero di giocatori + 4 per ripristinare la fila superiore
        int cardsToDrawUpper = this.numPlayers + 4;

        for (int i = 0; i < cardsToDrawUpper; i++) {
            if (!deck.isEmpty()) {
                // Rimuove la prima carta dal mazzo (pesca) e la aggiunge alla fila superiore
                this.upperRow.add(deck.removeFirst());
            }
        }
    }

    /**
     * Rimuove carte specifiche dal tabellone (es. quando un giocatore le acquista).
     */
    public void remove(List<Card> cardsToRemove) {
        // Rimuove le carte acquistate sia dalla riga superiore che inferiore
        this.upperRow.removeAll(cardsToRemove);
        this.lowerRow.removeAll(cardsToRemove);
        this.buildingMarket.removeAll(cardsToRemove);
    }

    /**
     * Fase di fine round: sposta le carte dalla fila superiore a quella inferiore.
     * Metodo chiamato UpToLow() nel tuo UML.
     */
    public void upToLow() {
        // Regola: Spostate nella fila inferiore tutte le carte Personaggio ed Eventi rimaste nella fila superiore
        // Le eventuali carte Edificio rimangono al loro posto

        Iterator<Card> iterator = this.upperRow.iterator();
        while (iterator.hasNext()) {
            Card c = iterator.next();
            if (!(c instanceof Building)) {
                this.lowerRow.add(c);
                iterator.remove(); // Rimuove in modo sicuro la carta dalla fila superiore
            }
        }
    }

    public List<Card> getUpperRow() {
        return upperRow;
    }

    public List<Card> getLowerRow() {
        return lowerRow;
    }

    /**
     * Metodo extra consigliato: pulisce la riga inferiore a fine round.
     */
    public void clearLowerRow(int Era) {
        // Regola: Scartate tutte le carte Personaggio ed Evento dalla fila inferiore[cite: 212].
        // Gli Edifici rimangono al loro posto[cite: 213].
        this.lowerRow.removeIf(c -> !(c instanceof Building));

        //Se currentEra = 3, gli eventuali edifici di era 1 vanno eliminati.


    }
}
