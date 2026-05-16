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

    public void addObserver(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (GameObserver observer : observers) {
            try {
                observer.gameStatusChanged(this);
            } catch (Exception e) {
                System.err.println("Error notifying observer: " + e.getMessage());
            }
        }
    }
    public int getObserverCount() {
        return observers.size();
    }
    //end observer methods

    /**
     * After deserialization the transient {@link #observers} list is null:
     * re-create it empty so a restored game can accept fresh observers.
     */
    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.observers = new CopyOnWriteArrayList<>();
    }

    public void setState(GameState state) {
        this.currentState = state;
        notifyObservers();
    }

    public void startMatch() {
        currentState.startMatch(this);
        notifyObservers();
    }

    public void placeTotemOnOffer(Player player, OfferTile tile) {
        currentState.placeTotemOnOffer(this, player, tile);
        notifyObservers();
    }

    public void pickCards(Player player, List<Card> selectedCards) {
        currentState.pickCards(this, player, selectedCards);
        notifyObservers();
    }

    // Delegate for the end-of-round bonus move
    public void pickBonusCard(Player player, Card bonusCard) {
        currentState.pickBonusCard(this, player, bonusCard);
        notifyObservers();
    }

    public void resolveEvents() {
        currentState.resolveEvents(this);
        notifyObservers();
    }

    public void updateRound() {
        currentState.updateRound(this);
        notifyObservers();
    }

    public Player determineWinner() {
        Player winner = currentState.determineWinner(this);
        notifyObservers();
        return winner;
    }

    public String getCurrentPhaseName() {
        return currentState.getPhaseName();
    }

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

    private static List<Card> shuffledByEra(List<Card> source, Era era) {
        List<Card> ofEra = new ArrayList<>(source.stream()
                .filter(c -> c.getEra() == era).toList());
        Collections.shuffle(ofEra);
        return ofEra;
    }

    private static int[] buildingsPerEra(int playerCount) {
        return switch (playerCount) {
            case 2 -> new int[]{1, 2, 3};
            case 3 -> new int[]{2, 2, 4};
            case 4 -> new int[]{2, 3, 4};
            case 5 -> new int[]{2, 3, 5};
            default -> throw new IllegalStateException("Invalid number of players: " + playerCount);
        };
    }
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

    public List<Player> getPlayers() { return players; }
    public Board getBoard() { return board; }
    public int getCurrentRound() { return currentRound; }
    public Era getCurrentEra() { return currentEra; }
    public Player getActivePlayer() { return activePlayer; }
    public List<Card> getTribeDeck() { return tribeDeck; }
    public GameState getCurrentState() {return currentState;}

    public void setActivePlayer(Player activePlayer) {
        this.activePlayer = activePlayer;
        notifyObservers();
    }

    public void setCurrentEra(Era currentEra) {
        Era oldEra = this.currentEra;
        this.currentEra = currentEra;
        if (oldEra != currentEra) {
            notifyObservers();
        }
    }

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
