package it.polimi.ingsw.am22.view;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameObserver;

/**
 * Example view implementation demonstrating the Observer pattern in MVC.
 * 
 * This view:
 * 1. Implements GameObserver to receive model updates
 * 2. Receives game state snapshots from GameController (Controller)
 * 3. Updates the UI based on the received state
 * 
 * Architecture flow:
 * Game (Model) -> notifies -> GameController (Controller) -> updates -> GameView (View)
 */
public class GameView implements GameObserver {

    @Override
    public void gameStatusChanged(Game game) {
    }
}