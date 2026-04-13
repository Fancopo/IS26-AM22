package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.states.SetUpState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class Game {
    private final List<Player> players;
    private final Board board;
    private final List<Card> tribeDeck;
    private final List<Building> buildingMarket;
    private int currentRound;
    private Era currentEra;
    private Player activePlayer;
    private GameState currentState;
    private List<GameObserver> observers;

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

    // INSERIMENTO del delegato per la mossa bonus di fine round
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

        List<Card> allTribeCards = new ArrayList<>();
        allTribeCards.addAll(loadedCards.getTribeCharacters());
        allTribeCards.addAll(loadedCards.getEvents());

        List<Card> filteredTribe = new ArrayList<>(allTribeCards.stream()
                .filter(card -> card.getMinPlayers() <= players.size())
                .toList());

        List<Card> era1 = new ArrayList<>(filteredTribe.stream()
                .filter(card -> card.getEra() == Era.I)
                .toList());

        List<Card> era2 = new ArrayList<>(filteredTribe.stream()
                .filter(card -> card.getEra() == Era.II)
                .toList());

        List<Card> era3 = new ArrayList<>(filteredTribe.stream()
                .filter(card -> card.getEra() == Era.III)
                .toList());

        List<Card> finalEvents = new ArrayList<>(loadedCards.getFinalEvents().stream()
                .filter(card -> card.getMinPlayers() <= players.size())
                .toList());

        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents);

        tribeDeck.clear();
        tribeDeck.addAll(era1);
        tribeDeck.addAll(era2);
        tribeDeck.addAll(era3);
        tribeDeck.addAll(finalEvents);

        List<Building> allBuildings = new ArrayList<>(loadedCards.getBuildings());

        List<Building> buildEra1 = new ArrayList<>(allBuildings.stream()
                .filter(building -> building.getEra() == Era.I)
                .toList());

        List<Building> buildEra2 = new ArrayList<>(allBuildings.stream()
                .filter(building -> building.getEra() == Era.II)
                .toList());

        List<Building> buildEra3 = new ArrayList<>(allBuildings.stream()
                .filter(building -> building.getEra() == Era.III)
                .toList());

        Collections.shuffle(buildEra1);
        Collections.shuffle(buildEra2);
        Collections.shuffle(buildEra3);

        int countEra1;
        int countEra2;
        int countEra3;

        switch (players.size()) {
            case 2 -> {
                countEra1 = 1;
                countEra2 = 2;
                countEra3 = 3;
            }
            case 3 -> {
                countEra1 = 2;
                countEra2 = 2;
                countEra3 = 4;
            }
            case 4 -> {
                countEra1 = 2;
                countEra2 = 3;
                countEra3 = 4;
            }
            case 5 -> {
                countEra1 = 2;
                countEra2 = 3;
                countEra3 = 5;
            }
            default -> throw new IllegalStateException("Numero giocatori non valido: " + players.size());
        }

        List<Building> selectedEra1 = new ArrayList<>(buildEra1.subList(0, countEra1));
        List<Building> selectedEra2 = new ArrayList<>(buildEra2.subList(0, countEra2));
        List<Building> selectedEra3 = new ArrayList<>(buildEra3.subList(0, countEra3));

        buildingMarket.clear();
        buildingMarket.addAll(selectedEra2);
        buildingMarket.addAll(selectedEra3);

        board.revealNewBuildings(selectedEra1);
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
    public Player getCurrentTurnPlayer() {return activePlayer;}

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
        return !(currentState instanceof SetUpState);
    }

    /**
     * Checks if the game has ended.
     *
     * @return true if the game is in the end game state
     */
    public boolean isGameEnded() {
        return currentState.getClass().getSimpleName().contains("EndGame");
    }
}