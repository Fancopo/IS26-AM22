package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EndGameStateTest {

    private final EndGameState state = new EndGameState();

    @Test
    void winnerIsThePlayerWithMostPoints() {
        Game game = gameWith(player("Alice", 10, 0), player("Bob", 5, 0));

        Player winner = state.determineWinner(game);

        assertEquals("Alice", winner.getNickname());
    }

    @Test
    void tieOnPointsIsBrokenByFood() {
        Game game = gameWith(player("Alice", 10, 3), player("Bob", 10, 5));

        Player winner = state.determineWinner(game);

        assertEquals("Bob", winner.getNickname(), "Same PP -> the player with more food wins");
    }

    @Test
    void tieOnPointsAndFoodKeepsTheFirstPlayerFound() {
        Game game = gameWith(player("Alice", 10, 4), player("Bob", 10, 4));

        Player winner = state.determineWinner(game);

        assertEquals("Alice", winner.getNickname());
    }

    @Test
    void negativeScoresStillProduceAWinner() {
        Game game = gameWith(player("Alice", -5, 0), player("Bob", -2, 0));

        Player winner = state.determineWinner(game);

        assertEquals("Bob", winner.getNickname());
    }

    @Test
    void noPlayersYieldsNoWinner() {
        Game game = new Game(new ArrayList<>());

        assertNull(state.determineWinner(game));
    }

    @Test
    void exposesPhaseNameAndEndGameFlag() {
        assertEquals("Game Over", state.getPhaseName());
        assertTrue(state.isEndGame());
        assertFalse(state.isSetupPhase());
    }

    // ==================== helpers ====================

    private Game gameWith(Player... players) {
        return new Game(new ArrayList<>(List.of(players)));
    }

    private Player player(String name, int pp, int food) {
        Player p = new Player(name);
        p.addPP(pp);
        p.addFood(food);
        return p;
    }
}
