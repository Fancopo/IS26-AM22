package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.states.SetUpState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


/**
 * Root of the game model and the entry point for every game action. It holds the
 * players, the {@link Board}, the decks and the current {@link GameState}, and it
 * delegates each action ({@code startMatch}, {@code pickCards}, …) to that state
 * (State pattern). After every change it notifies its {@link GameObserver}s.
 */
public class Game implements Serializable {
    private final List<Player> players;
    private final Board board;
    private final List<Card> tribeDeck;
    private final List<Building> buildingMarket;
    private int currentRound;
    private Era currentEra;
    private Player activePlayer;
    private GameState currentState;

    /**
     * Observers are runtime-only wiring (e.g. the server's VirtualView): they
     * are not part of the persisted game state. Marked transient so a saved
     * snapshot stays free of network/view objects; re-populated on restore by
     * whoever re-attaches as observer.
     */
    private transient List<GameObserver> observers;

    /**
     * Creates a new game in the initial setup phase.
     *
     * @param players the participating players (copied defensively)
     */
    public Game(List<Player> players) {
        this.players = new ArrayList<>(players);
        this.board = new Board(players.size());
        this.tribeDeck = new ArrayList<>();
        this.buildingMarket = new ArrayList<>();
        this.observers = new CopyOnWriteArrayList<>();
        this.currentRound = 1;
        this.currentEra = Era.I;

        this.currentState = new SetUpState();
    }

    /**
     * Registers an observer (ignored if null or already registered).
     *
     * @param observer the observer to add
     */
    public void addObserver(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * Unregisters an observer.
     *
     * @param observer the observer to remove
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    /** Notifies every registered observer; an exception in one does not stop the others. */
    public void notifyObservers() {
        for (GameObserver observer : observers) {
            try {
                observer.gameStatusChanged(this);
            } catch (Exception e) {
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }

    /** @return the number of currently registered observers */
    public int getObserverCount() {
        return observers.size();
    }
    //end observer methods

    /**
     * After deserialization the transient {@link #observers} list is null:
     * re-create it empty so a restored game can accept fresh observers.
     *
     * @param in the stream being read
     * @throws IOException            if reading fails
     * @throws ClassNotFoundException if a serialized class cannot be found
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.observers = new CopyOnWriteArrayList<>();
    }

    /**
     * Replaces the current state and notifies observers.
     *
     * @param state the new state
     */
    public void setState(GameState state) {
        this.currentState = state;
        notifyObservers();
    }

    /** Starts the match by delegating to the current state. */
    public void startMatch() {
        currentState.startMatch(this);
        notifyObservers();
    }

    /**
     * Places a player's totem on an offer tile by delegating to the current state.
     *
     * @param player the player acting
     * @param tile   the chosen offer tile
     */
    public void placeTotemOnOffer(Player player, OfferTile tile) {
        currentState.placeTotemOnOffer(this, player, tile);
        notifyObservers();
    }

    /**
     * Resolves a player's card pick by delegating to the current state.
     *
     * @param player        the player acting
     * @param selectedCards the cards the player chose
     */
    public void pickCards(Player player, List<Card> selectedCards) {
        currentState.pickCards(this, player, selectedCards);
        notifyObservers();
    }

    /**
     * Resolves the end-of-round bonus pick by delegating to the current state.
     *
     * @param player    the player entitled to the bonus pick
     * @param bonusCard the chosen card
     */
    public void pickBonusCard(Player player, Card bonusCard) {
        currentState.pickBonusCard(this, player, bonusCard);
        notifyObservers();
    }

    /** Resolves the round's events by delegating to the current state. */
    public void resolveEvents() {
        currentState.resolveEvents(this);
        notifyObservers();
    }

    /** Performs the end-of-round update by delegating to the current state. */
    public void updateRound() {
        currentState.updateRound(this);
        notifyObservers();
    }

    /**
     * Determines the winner by delegating to the current state.
     *
     * @return the winning player
     */
    public Player determineWinner() {
        Player winner = currentState.determineWinner(this);
        notifyObservers();
        return winner;
    }

    /** @return the name of the current phase */
    public String getCurrentPhaseName() {
        return currentState.getPhaseName();
    }

    /**
     * Loads the cards from the JSON resources and builds the tribe deck (shuffled
     * Era by Era, with the final events at the bottom) and the building market,
     * revealing the Era I buildings on the board. Player count drives how many
     * cards and buildings are used.
     */
    public void setupDecks() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadAllCards("/TribeCharacter-Event.json", "/Building.json");

        int playerCount = players.size();
        List<Card> tribeCards = new ArrayList<>();
        tribeCards.addAll(loadedCards.getTribeCharacters());
        tribeCards.addAll(loadedCards.getEvents());
        tribeCards.removeIf(c -> c.getMinPlayers() > playerCount);

        tribeDeck.clear();
        for (Era era : Era.values()) {
            List<Card> shuffled = shuffledByEra(tribeCards, era);
            tribeDeck.addAll(shuffled);
        }

        List<Card> finalEvents = new ArrayList<>(loadedCards.getFinalEvents());
        finalEvents.removeIf(c -> c.getMinPlayers() > playerCount);
        Collections.shuffle(finalEvents);
        tribeDeck.addAll(finalEvents);

        int[] buildingsPerEra = buildingsPerEra(playerCount);
        List<Building> allBuildings = loadedCards.getBuildings();
        List<List<Building>> byEra = new ArrayList<>();
        for (Era era : Era.values()) {
            List<Building> ofEra = new ArrayList<>(allBuildings.stream()
                    .filter(b -> b.getEra() == era).toList());
            Collections.shuffle(ofEra);
            byEra.add(ofEra);
        }

        List<Building> eraI = byEra.get(0).subList(0, buildingsPerEra[0]);
        List<Building> eraII = byEra.get(1).subList(0, buildingsPerEra[1]);
        List<Building> eraIII = byEra.get(2).subList(0, buildingsPerEra[2]);

        buildingMarket.clear();
        buildingMarket.addAll(eraII);
        buildingMarket.addAll(eraIII);
        board.revealNewBuildings(new ArrayList<>(eraI));
    }

    // Returns the cards of the given Era, shuffled.
    private static List<Card> shuffledByEra(List<Card> source, Era era) {
        List<Card> ofEra = new ArrayList<>(source.stream()
                .filter(c -> c.getEra() == era).toList());
        Collections.shuffle(ofEra);
        return ofEra;
    }

    // Number of buildings revealed per Era, by player count.
    private static int[] buildingsPerEra(int playerCount) {
        return switch (playerCount) {
            case 2 -> new int[]{1, 2, 3};
            case 3 -> new int[]{2, 2, 4};
            case 4 -> new int[]{2, 3, 4};
            case 5 -> new int[]{2, 3, 5};
            default -> throw new IllegalStateException("Invalid number of players: " + playerCount);
        };
    }

    /**
     * Applies an Era change: clears Era-III-destroyed buildings (only on Era III),
     * shifts surviving buildings down, and reveals the market buildings of the new
     * Era onto the board.
     */
    public void handleEraChange() {
        if (currentEra == Era.III) {
            board.clearLowerBuildings();
        }
        board.shiftBuildingsDown();

        List<Building> buildingsToReveal = new ArrayList<>();
        for (Building b : buildingMarket) {
            if (b.getEra() == currentEra) {
                buildingsToReveal.add(b);
            }
        }

        buildingMarket.removeAll(buildingsToReveal);
        board.revealNewBuildings(buildingsToReveal);
    }

    /**
     * @return the player whose totem occupies the leftmost (lowest-letter) offer
     *         tile, or {@code null} if the offer track is empty
     */
    public Player getPlayerWithLeftmostTotem() {
        for (OfferTile tile : board.getOfferTrack()) {
            if (!tile.isAvailable()) {
                for (Player p : players) {
                    if (p.getTotem() == tile.getOccupiedBy()) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** @return the players, in current turn order */
    public List<Player> getPlayers() { return players; }

    /** @return the game board */
    public Board getBoard() { return board; }

    /** @return the current round number (1-10) */
    public int getCurrentRound() { return currentRound; }

    /** @return the current Era */
    public Era getCurrentEra() { return currentEra; }

    /** @return the player whose turn it is, or {@code null} */
    public Player getActivePlayer() { return activePlayer; }

    /** @return the tribe deck (cards still to be drawn) */
    public List<Card> getTribeDeck() { return tribeDeck; }

    /** @return the current game state */
    public GameState getCurrentState() {return currentState;}

    /**
     * Sets the active player and notifies observers.
     *
     * @param activePlayer the new active player
     */
    public void setActivePlayer(Player activePlayer) {
        this.activePlayer = activePlayer;
        notifyObservers();
    }

    /**
     * Sets the current Era, notifying observers only if it actually changed.
     *
     * @param currentEra the new Era
     */
    public void setCurrentEra(Era currentEra) {
        Era oldEra = this.currentEra;
        this.currentEra = currentEra;
        if (oldEra != currentEra) {
            notifyObservers();
        }
    }

    /**
     * Sets the current round, notifying observers only if it actually changed.
     *
     * @param currentRound the new round number
     */
    public void setCurrentRound(int currentRound) {
        int oldRound = this.currentRound;
        this.currentRound = currentRound;
        if (oldRound != currentRound) {
            notifyObservers();
        }
    }

    /**
     * Checks if the game has started (past setup phase).
     *
     * @return true if the game is in progress
     */
    public boolean isGameStarted() {
        return !currentState.isSetupPhase();
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the game is in the end game state
     */
    public boolean isGameEnded() {
        return currentState.isEndGame();
    }
}
