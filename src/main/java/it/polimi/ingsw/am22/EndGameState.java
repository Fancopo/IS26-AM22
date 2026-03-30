package it.polimi.ingsw.am22;

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

        game.notifyObservers(); // Notifica la fine della partita e il vincitore
        return winner;
    }

    @Override
    public String getPhaseName() { return "Fine Partita"; }
}