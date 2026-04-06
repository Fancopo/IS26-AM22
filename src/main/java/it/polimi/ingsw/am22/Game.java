package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Game {
    private List<Player> players;
    private Board board;
    private List<Card> tribeDeck;
    private List<Building> buildingMarket;
    private int currentRound;
    private Era currentEra;
    private Player activePlayer;
    private GameState currentState;
    //private List<GameObserver> observers;

    public Game(List<Player> players) {
        this.players = players;
        this.board = new Board(players.size());
        this.tribeDeck = new ArrayList<>();
        this.buildingMarket = new ArrayList<>();
        //this.observers = new ArrayList<>();
        this.currentRound = 1;
        this.currentEra = Era.I;

        this.currentState = new SetUpState();
    }

    /*public void addObserver(GameObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (GameObserver observer : observers) {
            observer.gameStatusChanged(this);
        }
    }*/

    public void setState(GameState state) {
        this.currentState = state;
    }

    public void startMatch() {
        currentState.startMatch(this);
    }

    public void placeTotemOnOffer(Player player, OfferTile tile) {
        currentState.placeTotemOnOffer(this, player, tile);
    }

    public void pickCards(Player player, List<Card> selectedCards) {
        currentState.pickCards(this, player, selectedCards);
    }

    // INSERIMENTO del delegato per la mossa bonus di fine round
    public void pickBonusCard(Player player, Card bonusCard) {
        currentState.pickBonusCard(this, player, bonusCard);
    }

    public void resolveEvents() {
        currentState.resolveEvents(this);
    }

    public void updateRound() {
        currentState.updateRound(this);
    }

    public Player determineWinner() {
        return currentState.determineWinner(this);
    }

    public String getCurrentPhaseName() {
        return currentState.getPhaseName();
    }

    void setupDecks() {
        List<Card> allTribeCards = CardDeck.createAllTribeCards();

        List<Card> filteredTribe = allTribeCards.stream()
                .filter(c -> c.getMinPlayers() <= players.size())
                .collect(Collectors.toList());

        List<Card> era1 = filteredTribe.stream().filter(c -> c.getEra() == Era.I).collect(Collectors.toList());
        List<Card> era2 = filteredTribe.stream().filter(c -> c.getEra() == Era.II).collect(Collectors.toList());
        List<Card> era3 = filteredTribe.stream().filter(c -> c.getEra() == Era.III).collect(Collectors.toList());
        List<Card> finalEvents = filteredTribe.stream().filter(c -> c.isFinalEvent()).collect(Collectors.toList());

        Collections.shuffle(era1);
        Collections.shuffle(era2);
        Collections.shuffle(era3);
        Collections.shuffle(finalEvents);

        this.tribeDeck.addAll(finalEvents);
        this.tribeDeck.addAll(era3);
        this.tribeDeck.addAll(era2);
        this.tribeDeck.addAll(era1);

        List<Building> allBuildings = CardDeck.createAllBuildings();

        List<Building> buildEra1 = allBuildings.stream().filter(b -> b.getEra() == Era.I).collect(Collectors.toList());
        List<Building> buildEra2 = allBuildings.stream().filter(b -> b.getEra() == Era.II).collect(Collectors.toList());
        List<Building> buildEra3 = allBuildings.stream().filter(b -> b.getEra() == Era.III).collect(Collectors.toList());

        Collections.shuffle(buildEra1);
        Collections.shuffle(buildEra2);
        Collections.shuffle(buildEra3);

        int countEra1 = 0, countEra2 = 0, countEra3 = 0;
        switch (players.size()) {
            case 2: countEra1 = 1; countEra2 = 2; countEra3 = 3; break;
            case 3: countEra1 = 2; countEra2 = 2; countEra3 = 4; break;
            case 4: countEra1 = 2; countEra2 = 3; countEra3 = 4; break;
            case 5: countEra1 = 2; countEra2 = 3; countEra3 = 5; break;
        }

        List<Building> selectedEra1 = new ArrayList<>(buildEra1.subList(0, countEra1));
        List<Building> selectedEra2 = new ArrayList<>(buildEra2.subList(0, countEra2));
        List<Building> selectedEra3 = new ArrayList<>(buildEra3.subList(0, countEra3));

        this.buildingMarket.addAll(selectedEra2);
        this.buildingMarket.addAll(selectedEra3);
        board.revealNewBuildings(selectedEra1);
    }

    void handleEraChange() {
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

    Player getPlayerWithLeftmostTotem() {
        for (OfferTile tile : board.getOfferTrack()) {
            if (!tile.isAvailable()) {
                for (Player p : players) {
                    if (p.getTotem() == tile.getOccupyingTotem()) {
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

    public void setActivePlayer(Player activePlayer) { this.activePlayer = activePlayer; }
    public void setCurrentEra(Era currentEra) { this.currentEra = currentEra; }
    public void setCurrentRound(int currentRound) { this.currentRound = currentRound; }
}