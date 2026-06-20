package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.Player;

/**
 * Terminal phase of the game. The only action available is determining the
 * winner: the player with the most final points, with food used as the
 * tie-breaker.
 */
public class EndGameState implements GameState {

    /**
     * Finds the winner by highest {@link Player#finalPP()}, breaking ties by food.
     *
     * @param game the finished game
     * @return the winning player, or {@code null} if there are no players
     */
    @Override
    public Player determineWinner(Game game) {
        Player winner = null;
        int maxPP = -999;
        int maxFood = -1;

        for (Player p : game.getPlayers()) {
            int currentFinalPP = p.finalPP();
            if (currentFinalPP > maxPP) {
                maxPP = currentFinalPP;
                maxFood = p.getFood();
                winner = p;
            } else if (currentFinalPP == maxPP) {
                if (p.getFood() > maxFood) {
                    maxFood = p.getFood();
                    winner = p;
                }
            }
        }
        return winner;
    }

    @Override
    public String getPhaseName() { return "Game Over"; }

    @Override
    public boolean isEndGame() { return true; }
}
