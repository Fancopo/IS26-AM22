package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.states.SetUpState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GameTest {

    private List<Player> players;
    private Game game;

    @BeforeEach
    void setUp() {
        players = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            Player player = new Player("Player" + i);
            player.setTotem(new Totem("color" + i, player));
            players.add(player);
        }
        game = new Game(players);
    }

    // ==================== CONSTRUCTOR TESTS ====================

    @Test
    void constructorShouldInitializeGameCorrectly() {
        assertNotNull(game);
        assertEquals(3, game.getPlayers().size());
        assertNotNull(game.getBoard());
        assertNotNull(game.getTribeDeck());
        assertEquals(1, game.getCurrentRound());
        assertEquals(Era.I, game.getCurrentEra());
        assertNull(game.getActivePlayer());
        assertTrue(game.getCurrentState() instanceof SetUpState);
    }

    @Test
    void constructorShouldInitializeEmptyDecks() {
        assertTrue(game.getTribeDeck().isEmpty());
    }

    @Test
    void constructorWithTwoPlayers() {
        List<Player> twoPlayers = new ArrayList<>();
        twoPlayers.add(createPlayer("P1", "red"));
        twoPlayers.add(createPlayer("P2", "blue"));

        Game twoPlayerGame = new Game(twoPlayers);

        assertEquals(2, twoPlayerGame.getPlayers().size());
    }

    @Test
    void constructorWithFivePlayers() {
        List<Player> fivePlayers = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            fivePlayers.add(createPlayer("P" + i, "color" + i));
        }
        Game fivePlayerGame = new Game(fivePlayers);

        assertEquals(5, fivePlayerGame.getPlayers().size());
    }

    // ==================== OBSERVER PATTERN TESTS ====================

    @Test
    void addObserverShouldRegisterObserver() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        assertEquals(1, game.getObserverCount());
    }

    @Test
    void addObserverShouldNotAddDuplicateObservers() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        game.addObserver(observer);
        assertEquals(1, game.getObserverCount());
    }

    @Test
    void addObserverShouldRejectNullObserver() {
        game.addObserver(null);
        assertEquals(0, game.getObserverCount());
    }

    @Test
    void removeObserverShouldUnregisterObserver() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        game.removeObserver(observer);
        assertEquals(0, game.getObserverCount());
    }

    @Test
    void removeObserverShouldDoNothingIfObserverNotRegistered() {
        FakeGameObserver observer = new FakeGameObserver();
        game.removeObserver(observer);
        assertEquals(0, game.getObserverCount());
    }

    @Test
    void notifyObserversShouldCallAllObservers() {
        FakeGameObserver observer1 = new FakeGameObserver();
        FakeGameObserver observer2 = new FakeGameObserver();
        game.addObserver(observer1);
        game.addObserver(observer2);

        game.notifyObservers();

        assertEquals(1, observer1.notificationCount);
        assertEquals(1, observer2.notificationCount);
    }

    @Test
    void notifyObserversShouldHandleObserverExceptions() {
        GameObserver badObserver = new GameObserver() {
            @Override
            public void gameStatusChanged(Game game) {
                throw new RuntimeException("Test exception");
            }
        };
        FakeGameObserver goodObserver = new FakeGameObserver();

        game.addObserver(badObserver);
        game.addObserver(goodObserver);

        assertDoesNotThrow(() -> game.notifyObservers());
        assertEquals(1, goodObserver.notificationCount);
    }

    @Test
    void notifyObserversShouldBeCalledOnStartMatch() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        game.setState(new FakeGameState()); // Previene caricamento JSON reale

        game.startMatch();

        assertTrue(observer.notificationCount > 0);
    }

    // ==================== STATE MANAGEMENT TESTS ====================

    @Test
    void setStateShouldChangeGameState() {
        FakeGameState newState = new FakeGameState();
        newState.phaseName = "Test Phase";

        game.setState(newState);

        assertEquals(newState, game.getCurrentState());
        assertEquals("Test Phase", game.getCurrentPhaseName());
    }

    @Test
    void getCurrentStateShouldReturnInitialState() {
        assertTrue(game.getCurrentState() instanceof SetUpState);
    }

    // ==================== GAME LIFECYCLE DELEGATION TESTS ====================

    @Test
    void startMatchShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);

        game.startMatch();

        assertEquals(1, fakeState.startMatchCalls);
    }

    @Test
    void placeTotemOnOfferShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);
        Player player = players.get(0);

        game.placeTotemOnOffer(player, null); // Passiamo null dato che testiamo solo la delega

        assertEquals(1, fakeState.placeTotemCalls);
    }

    @Test
    void pickCardsShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);
        Player player = players.get(0);

        game.pickCards(player, new ArrayList<>());

        assertEquals(1, fakeState.pickCardsCalls);
    }

    @Test
    void pickBonusCardShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);
        Player player = players.get(0);

        game.pickBonusCard(player, null);

        assertEquals(1, fakeState.pickBonusCardCalls);
    }

    @Test
    void resolveEventsShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);

        game.resolveEvents();

        assertEquals(1, fakeState.resolveEventsCalls);
    }

    @Test
    void updateRoundShouldDelegateToState() {
        FakeGameState fakeState = new FakeGameState();
        game.setState(fakeState);

        game.updateRound();

        assertEquals(1, fakeState.updateRoundCalls);
    }

    @Test
    void determineWinnerShouldDelegateToStateAndReturnWinner() {
        FakeGameState fakeState = new FakeGameState();
        Player expectedWinner = players.get(0);
        fakeState.winnerToReturn = expectedWinner;
        game.setState(fakeState);

        Player winner = game.determineWinner();

        assertEquals(expectedWinner, winner);
        assertEquals(1, fakeState.determineWinnerCalls);
    }

    // ==================== GETTER & SETTER TESTS ====================

    @Test
    void setActivePlayerShouldUpdateActivePlayer() {
        Player player = players.get(0);
        game.setActivePlayer(player);
        assertEquals(player, game.getActivePlayer());
    }

    @Test
    void setCurrentEraShouldUpdateEra() {
        game.setCurrentEra(Era.II);
        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void setCurrentRoundShouldUpdateRound() {
        game.setCurrentRound(5);
        assertEquals(5, game.getCurrentRound());
    }

    // ==================== UTILITY METHOD TESTS ====================

    @Test
    void isGameStartedShouldReturnFalseInitially() {
        assertFalse(game.isGameStarted());
    }

    @Test
    void isGameStartedShouldReturnTrueAfterStateChange() {
        game.setState(new FakeGameState());
        assertTrue(game.isGameStarted());
    }

    @Test
    void getCurrentTurnPlayerShouldReturnActivePlayer() {
        Player player = players.get(0);
        game.setActivePlayer(player);
        assertEquals(player, game.getCurrentTurnPlayer());
    }

    // ==================== INTEGRATION & EDGE CASE TESTS ====================

    @Test
    void fullGameFlowShouldWorkCorrectly() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        game.setState(new FakeGameState()); // Ignora file reali

        game.startMatch();
        game.setActivePlayer(players.get(0));
        assertNotNull(game.getActivePlayer());

        game.setCurrentRound(2);
        assertEquals(2, game.getCurrentRound());

        game.setCurrentEra(Era.II);
        assertEquals(Era.II, game.getCurrentEra());

        assertTrue(observer.notificationCount >= 3);
    }

    @Test
    void gameWithMinimumPlayers() {
        List<Player> minPlayers = new ArrayList<>();
        minPlayers.add(createPlayer("P1", "red"));
        minPlayers.add(createPlayer("P2", "blue"));

        Game minGame = new Game(minPlayers);

        assertEquals(2, minGame.getPlayers().size());
        assertNotNull(minGame.getBoard());
    }

    @Test
    void notifyObserversShouldBeCalledAfterEveryMajorAction() {
        FakeGameObserver observer = new FakeGameObserver();
        game.addObserver(observer);
        game.setState(new FakeGameState());

        observer.notificationCount = 0; // reset
        game.startMatch();
        assertTrue(observer.notificationCount > 0);

        observer.notificationCount = 0; // reset
        game.placeTotemOnOffer(players.get(0), null);
        assertTrue(observer.notificationCount > 0);

        observer.notificationCount = 0; // reset
        game.resolveEvents();
        assertTrue(observer.notificationCount > 0);
    }

    // ==================== HELPER METHODS ====================

    private Player createPlayer(String nickname, String totemColor) {
        Player player = new Player(nickname);
        player.setTotem(new Totem(totemColor, player));
        return player;
    }

    // ==================== MANUAL MOCKS (FAKES) ====================

    /**
     * Finta classe Observer per contare quante volte viene notificata.
     */
    private static class FakeGameObserver implements GameObserver {
        int notificationCount = 0;

        @Override
        public void gameStatusChanged(Game game) {
            notificationCount++;
        }
    }

    /**
     * Finto Stato del Gioco che registra semplicemente se i metodi vengono chiamati,
     * senza eseguire logica complessa.
     */
    private static class FakeGameState implements GameState {
        String phaseName = "Fake Phase";
        int startMatchCalls = 0;
        int placeTotemCalls = 0;
        int pickCardsCalls = 0;
        int pickBonusCardCalls = 0;
        int resolveEventsCalls = 0;
        int updateRoundCalls = 0;
        int determineWinnerCalls = 0;
        Player winnerToReturn = null;

        @Override public void startMatch(Game game) { startMatchCalls++; }
        @Override public void placeTotemOnOffer(Game game, Player player, OfferTile tile) { placeTotemCalls++; }
        @Override public void pickCards(Game game, Player player, List<Card> selectedCards) { pickCardsCalls++; }
        @Override public void pickBonusCard(Game game, Player player, Card bonusCard) { pickBonusCardCalls++; }
        @Override public void resolveEvents(Game game) { resolveEventsCalls++; }
        @Override public void updateRound(Game game) { updateRoundCalls++; }
        @Override public Player determineWinner(Game game) { determineWinnerCalls++; return winnerToReturn; }
        @Override public String getPhaseName() { return phaseName; }
    }
}