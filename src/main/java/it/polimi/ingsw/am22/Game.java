package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private int currentRound;
    private List<Player> players;
    private int currentEra;
    private Board board;
    private List<Card> tribeDeck;

    private GameObserver gameObserver;

    public Game(int num_players) {
        this.players = new ArrayList<>();
        for(int i=0;i<num_players;i++)
            this.players.add(new Player(i));
        }
    }

    public void startMatch() {
        this.currentRound = 1;
        this.currentEra = 1;
        this.board = new Board(players.size());

        // inizializza il tribeDeck
        List<Card> AllCards = CardDeck.createAllCards();

        // Filtriamo usando l'attributo minPlayers e gli Stream di Java
        this.tribeDeck = AllCards.stream()
                .filter(carta -> carta.getMinPlayers() <= num_Players)
                .collect(Collectors.toList());

        // Mescolo il mazzo pronto per l'uso
        Collections.shuffle(this.tribeDeck);

        //basic food distribution
        for (int i = 0; i < this.players.size(); i++) {
            Player p = this.players.get(i);
            if (i == 0) {
                p.addFood(2); // Il 1° giocatore riceve 2 Cibo
            } else if (i == 1 || i == 2) {
                p.addFood(3); // Il 2° e il 3° giocatore ricevono 3 Cibo
            } else if (i == 3 || i == 4) {
                p.addFood(4); // Il 4° e il 5° giocatore ricevono 4 Cibo
            }
        }

        this.board.initTrack();
        this.board.refill(this.tribeDeck);

        //carica carte nel lowerRow nel primo round
        int cardsToDrawLower = this.players.size() + 1;
        for (int i = 0; i < cardsToDrawLower; i++) {
            if (!this.tribeDeck.isEmpty()) {
                // Rimuove la prima carta dal mazzo (pesca) e la aggiunge alla fila superiore
                this.board.getLowerRow().add(deck.removeFirst());
            }
        }

        // Notifico l'interfaccia grafica che il setup è completato (?)
        if (this.gameObserver != null) {
            this.gameObserver.gameStatusChanged();
        }


    }

    public void resolveEvents() {

    }

    //nextRound
    public void updateRound() {
        // 1. Risoluzione degli Eventi
        resolveEvents();

        // 2. Pulizia e aggiornamento del tabellone (Passaggi 2, 3 e 4)
        this.board.clearLowerRow(); // Scarta Personaggi/Eventi dalla riga in basso
        this.board.upToLow();       // Sposta le carte dalla riga in alto a quella in basso
        this.board.refill(this.tribeDeck); // Pesca nuove carte per la riga in alto

        // 3. Controllo cambio Era
        // Se la prima carta del mazzo (o le nuove carte rivelate) sono della nuova Era,
        // aggiorniamo l'Era e modifichiamo il tabellone di conseguenza.
        updateEra();

        // 4. Incremento del contatore del round
        this.currentRound++;

        // 5. Controllo di Fine Partita
        // La partita finisce alla fine del 10° round.
        if (this.currentRound > 10 || this.tribeDeck.isEmpty()) {
            for(player: players)
        } else {
            // Notifichiamo l'interfaccia che un nuovo round sta per iniziare
            if (this.gameObserver != null) {
                this.gameObserver.gameStatusChanged();
            }
        }
    }

    public void updateEra() {
        currentEra++;
    }
}
