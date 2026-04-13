package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.Player;

public class EndGameState implements GameState {

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
    public String getPhaseName() { return "Fine Partita"; }
}