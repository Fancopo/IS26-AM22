package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.List;

public class Game {
    // --- Attributes ---
    private List<Player> players;
    private Board board;
    private List<Card> tribeDeck;
    private int currentRound;
    private Era currentEra;
    private GamePhase currentPhase;
    private Player activePlayer;

    // Observer list for MVC pattern
    private List<GameObserver> observers;

    public Game(List<Player> players) {
        this.players = players;
        board = new Board(players.size());
        tribeDeck = new ArrayList<>();
        observers = new ArrayList<>();

        currentRound = 1;
        currentEra = Era.I;
        currentPhase = GamePhase.SETUP;

    }

    // --- OBSERVER PATTERN METHODS ---

    /**
     * Adds an observer (View/Client) to the notification list.
     * @param observer The observer to add.
     */
    public void addObserver(GameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Removes an observer from the notification list.
     * @param observer The observer to remove.
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notifies all registered observers that the game state has changed.
     */
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.gameStatusChanged(this);
        }
    }

    // --- GAME LOGIC METHODS ---
    private void setupDecks() {
        // ==========================================
        // 1. PREPARAZIONE MAZZO TRIBÙ
        // ==========================================

        List<Card> allTribeCards = CardDeck.createAllTribeCards();

        // Filtro per numero giocatori
        List<Card> filteredTribe = allTribeCards.stream()
                .filter(c -> c.getMinPlayers() <= players.size())
                .collect(Collectors.toList());

        // Separo le carte in base all'Era (o se sono Eventi Finali) usando gli Stream!
        List<Card> era1 = filteredTribe.stream().filter(c -> c.getEra() == Era.I).collect(Collectors.toList());
        List<Card> era2 = filteredTribe.stream().filter(c -> c.getEra() == Era.II).collect(Collectors.toList());
        List<Card> era3 = filteredTribe.stream().filter(c -> c.getEra() == Era.III).collect(Collectors.toList());
        List<Card> finalEvents = filteredTribe.stream().filter(c -> c.isFinalEvent()).collect(Collectors.toList());

        // Mescolo i mazzetti separatamente [cite: 143]
        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents);

        // Impilo il mazzo Tribù partendo dal basso verso l'alto [cite: 144]
        this.tribeDeck = new ArrayList<>();
        this.tribeDeck.addAll(finalEvents); // Fondo
        this.tribeDeck.addAll(era3);        // Sopra agli eventi finali
        this.tribeDeck.addAll(era2);        // Sopra l'Era III
        this.tribeDeck.addAll(era1);        // In cima, pronti per essere pescati al primo round!

        // ==========================================
        // 2. PREPARAZIONE MAZZI EDIFICIO [cite: 150-152, 164-165]
        // ==========================================

        List<Building> allBuildings = CardDeck.createAllBuildings();

        // Separo gli Edifici per Era usando gli Stream
        List<Building> buildEra1 = allBuildings.stream().filter(b -> b.getEra() == Era.I).collect(Collectors.toList());
        List<Building> buildEra2 = allBuildings.stream().filter(b -> b.getEra() == Era.II).collect(Collectors.toList());
        List<Building> buildEra3 = allBuildings.stream().filter(b -> b.getEra() == Era.III).collect(Collectors.toList());

        // Mescolo i mazzetti separatamente
        Collections.shuffle(buildEra1);
        Collections.shuffle(buildEra2);
        Collections.shuffle(buildEra3);

        // Determino quante carte prendere in base ai giocatori (Tabella del manuale)
        int countEra1 = 0;
        int countEra2 = 0;
        int countEra3 = 0;

        switch (players.size()) {
            case 2:
                countEra1 = 1; countEra2 = 2; countEra3 = 3;
                break;
            case 3:
                countEra1 = 2; countEra2 = 2; countEra3 = 4;
                break;
            case 4:
                countEra1 = 2; countEra2 = 3; countEra3 = 4;
                break;
            case 5:
                countEra1 = 2; countEra2 = 3; countEra3 = 5;
                break;
        }

        // Prendo solo il numero richiesto di carte (subList) in base allo switch
        List<Building> selectedEra1 = new ArrayList<>(buildEra1.subList(0, countEra1));
        List<Building> selectedEra2 = new ArrayList<>(buildEra2.subList(0, countEra2));
        List<Building> selectedEra3 = new ArrayList<>(buildEra3.subList(0, countEra3));

        // Gli edifici Era II e III vanno nella riserva coperta (BuildingMarket) pronti per le Ere successive [cite: 165]
        board.getBuildingMarket().addAll(selectedEra2);
        board.getBuildingMarket().addAll(selectedEra3);

        // Gli edifici Era I partono subito SCOPERTI nella fila superiore[cite: 164]!
        board.getUpperRow().addAll(selectedEra1);
    }
    /**
     * Starts the match, dealing initial food and setting up the board.
     */
    public void startMatch() {
        board.getTurnOrderTile().setup(players.size());
        board.initTrack();
        currentEra = board.refillUpperRow(tribeDeck,currentEra);
        setupDecks();

        // Randomize initial turn order by shuffling players
        Collections.shuffle(players);
        activePlayer = players.get(0);

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Slot slot = board.getTurnOrderTile().getSlots().get(i);
            slot.placeTotem(p.getTotem());
            p.getTotem().moveToSlot(slot);

            //Distribute initial food based on turn order
            if (i == 0) p.addFood(2);
            else if (i == 1 || i == 2) p.addFood(3);
            else if (i == 3 || i == 4) p.addFood(4);
        }

        //carica carte nel lowerRow nel primo round
        int cardsToDrawLower = players.size() + 1;
        for (int i = 0; i < cardsToDrawLower; i++) {
            if (!tribeDeck.isEmpty()) {
                // Rimuove la prima carta dal mazzo (pesca) e la aggiunge alla fila superiore
                board.getLowerRow().add(tribeDeck.remove(0));
            }
        }
        currentPhase = GamePhase.TOTEM_PLACEMENT;
        notifyObservers();
    }
    public void resolvePlayerOfferAction(Player player, OfferTile tile) {
        // 1. se il tile offre cibo
        if (tile.getFoodReward() > 0) {
            player.addFood(tile.getFoodReward());
        }

        // NOTE: Taking cards is usually handled by the Controller since it requires player choice.
        // The controller will call something like: board.takeUpperCard() and player.getTribe().addCharacter().

        // 2. Move Totem to the first available Turn Order Slot
        Slot nextSlot = board.getTurnOrderTile().getFirstAvailableSlot();
        tile.clear();
        nextSlot.placeTotem(player.getTotem());
        player.getTotem().moveToSlot(nextSlot);

        // 3. Apply Turn Order Slot bonuses/penalties
        if (nextSlot.getFoodBonus() > 0) {
            player.addFood(nextSlot.getFoodBonus());
        }
        if (nextSlot.isLastSpace()) {
            if (player.getFood() >= 1) {
                player.addFood(-1); // Pay 1 food
            } else {
                player.addPP(-2);   // Lose 2 PP if no food
            }
        }
        if (board.getTurnOrderTile().getOccupiedSlotsCount() == players.size()) {
            // Action phase is over. Move to Events! [cite: 274-275]
            this.currentPhase = GamePhase.EVENT_RESOLUTION;
            resolveEvents();
        } else {
            // The phase continues. Find the next leftmost totem on the track

            this.activePlayer = getPlayerWithLeftmostTotem();
        }
        notifyObservers();
    }
    public void resolveEvents() {
        currentPhase = GamePhase.EVENT_RESOLUTION;
        notifyObservers();

        List<Event> activeEvents = new ArrayList<>();
        // aggiungere eventi presenti nel lower row
        for (Card c : board.getLowerRow()) {
            if (c instanceof Event) {
                activeEvents.add((Event) c);
            }
        }

        // Mantenere sustenance per l'ultimo evento
        activeEvents.sort((e1, e2) -> {
            if (e1.getEventType() == EventType.SUSTENANCE) return 1;
            if (e2.getEventType() == EventType.SUSTENANCE) return -1;
            return 0; // Otherwise keep current order
        });

        // Apply each event to all players
        for (Event event : activeEvents) {
            event.getEffect().applyEvent(players); // Delegates to the specific EventEffect Strategy
        }

        notifyObservers();
    }

    //nextRound
    public void updateRound() {
        // 2. Pulizia e aggiornamento del tabellone (Passaggi 2, 3 e 4)
        board.clearLowerRow(); // Scarta Personaggi/Eventi dalla riga in basso
        board.shiftUpToLow();       // Sposta le carte dalla riga in alto a quella in basso
        Era eraAfterRefill = board.refillUpperRow(tribeDeck, currentEra); // Pesca nuove carte per la riga in alto

        // 3. Controllo cambio Era
        if (eraAfterRefill != currentEra) {
            currentEra = eraAfterRefill;
            handleEraChange(); // Chiama la logica per spostare gli Edifici
        }

        // 4. Update the active player order based on the new Totem positions
        List<Totem> newOrder = board.getTurnOrderTile().getTurnOrder();
        players.clear();
        for (Totem t : newOrder) {
            players.add(t.getOwner());
        }
        activePlayer = players.get(0);

        // 5. Check for end game [cite: 336]
        if (currentRound == 10) {
            currentPhase = GamePhase.END_GAME;
            determineWinner();
        } else {
            currentRound++;
            currentPhase = GamePhase.TOTEM_PLACEMENT;
        }
        notifyObservers();
    }

    public Player determineWinner() {
        currentPhase = GamePhase.END_GAME;

        Player winner = null;
        int maxPP = -999;
        int maxFood = -1;
        for (Player p : players) {
            int currentFinalPP = p.finalPP();
            // Trova chi ha più punti
            if (currentFinalPP > maxPP) {
                maxPP = currentFinalPP;
                maxFood = p.getFood();
                winner = p;
            }
            // Gestione dello spareggio in caso di parità di PP
            else if (currentFinalPP == maxPP) {
                if (p.getFood() > maxFood) {
                    // Vince il giocatore in parità con più cibo! [cite: 347]
                    maxFood = p.getFood();
                    winner = p;
                } else if (p.getFood() == maxFood) {
                    // In caso di ulteriore parità, la vittoria è condivisa(??)
                }
            }
        }
        notifyObservers();

        return winner;
    }

    public void placeTotemOnOffer(Player player, OfferTile tile) {
        // RIMUOVE il totem dal suo Slot attuale sulla TurnOrderTile
        for (Slot slot : board.getTurnOrderTile().getSlots()) {
            if (slot.getOccupiedBy() == player.getTotem()) {
                slot.removeTotem(); // (o il metodo che hai usato per svuotare lo slot, es. clear())
                break;
            }
        }
        // Place the totem on the chosen tile
        tile.placeTotem(player.getTotem());

        // 2. CHECK FOR PHASE CHANGE
        if (board.getTotemsOnOffersCount() == players.size()) {
            // Everyone has placed a totem! Move to the next phase
            this.currentPhase = GamePhase.ACTION_RESOLUTION;
            // The next active player is the one with the leftmost Totem on the Offer Track
            this.activePlayer = getPlayerWithLeftmostTotem();
        } else {
            // The phase continues. The next active player is the next one in the players list.
            int currentIndex = players.indexOf(player);
            this.activePlayer = players.get(currentIndex + 1);
        }

        notifyObservers(); //
    }
    private void handleEraChange() {
        if (currentEra == Era.III) {
            board.clearLowerBuildings(); // Scarta edifici in basso solo all'Era III
        }
        board.shiftBuildingsDown(); // Sposta gli edifici dall'alto al basso
        board.revealNewBuildings(currentEra); // Rivela i nuovi edifici dell'Era corrente
    }
    private Player getPlayerWithLeftmostTotem() {
        for (OfferTile tile : board.getOfferTrack()) {
            if (!tile.isAvailable()) { // If the tile has a totem
                // Assuming Totem has a getOwner() method
                // return tile.getOccupyingTotem().getOwner();

                // Temporary workaround based on your current UML if getOwner isn't there:
                for (Player p : players) {
                    if (p.getTotem() == tile.getOccupyingTotem()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }
    // --- GETTERS & SETTERS ---

    public List<Player> getPlayers() { return players; }
    public Board getBoard() { return board; }
    public int getCurrentRound() { return currentRound; }
    public Era getCurrentEra() { return currentEra; }
    public GamePhase getCurrentPhase() { return currentPhase; }
    public Player getActivePlayer() { return activePlayer; }

}
