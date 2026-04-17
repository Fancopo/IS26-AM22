package it.polimi.ingsw.am22.view;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameObserver;
import it.polimi.ingsw.am22.controller.GameController;

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
/* public class GameView implements GameObserver {
    
    private final GameController controller;
    private GameController.GameSnapshot currentSnapshot;
    
    public GameView(GameController controller) {
        this.controller = controller;
        
        // Register this view as an observer through the controller
        // The controller manages the subscription to model changes
        controller.addListener(snapshot -> {
            this.currentSnapshot = snapshot;
            updateDisplay();
        });
    }
    
    /**
     * Called by the Game model (via GameController) when state changes.
     * This is the Observer pattern in action.
     */
    @Override
    public void gameStatusChanged(Game game) {
        // The model changed, but we get our data from the controller's snapshot
        // This maintains proper MVC separation
        refreshFromController();
    }
    
    /**
     * Refreshes the view using the controller's snapshot.
     */
    private void refreshFromController() {
        if (controller.hasGame()) {
            this.currentSnapshot = controller.getSnapshot();
            updateDisplay();
        }
    }
    
    /**
     * Updates the UI based on the current game state.
     */
    private void updateDisplay() {
        if (currentSnapshot == null) {
            return;
        }
        
        // Update UI elements with current snapshot data
        System.out.println("=== Game State Update ===");
        System.out.println("Round: " + currentSnapshot.currentRound());
        System.out.println("Era: " + currentSnapshot.currentEra());
        System.out.println("Phase: " + currentSnapshot.currentPhase());
        System.out.println("Active Player: " + currentSnapshot.activePlayer());
        
        // Update player information
        currentSnapshot.players().forEach(player -> {
            System.out.println("  Player: " + player.nickname() + 
                             " | PP: " + player.prestigePoints() + 
                             " | Food: " + player.food() +
                             " | Active: " + player.active());
        });
        
        // Update board information
        System.out.println("Upper Row Cards: " + currentSnapshot.board().upperRow().size());
        System.out.println("Lower Row Cards: " + currentSnapshot.board().lowerRow().size());
    }
    
    /**
     * Example method showing how user actions go through the controller.
     */
    public void handlePlaceTotem(String playerNickname, char offerLetter) {
        try {
            controller.placeTotem(playerNickname, offerLetter);
            // No need to manually update - observer pattern handles it
        } catch (Exception e) {
            showError("Invalid move: " + e.getMessage());
        }
    }
    
    /**
     * Example method for picking cards.
     */
    public void handlePickCards(String playerNickname, java.util.Collection<String> cardIds) {
        try {
            controller.pickCards(playerNickname, cardIds);
            // View will be automatically updated via observer pattern
        } catch (Exception e) {
            showError("Invalid card selection: " + e.getMessage());
        }
    }
    
    /**
     * Displays error messages to the user.
     */
    private void showError(String message) {
        System.err.println("ERROR: " + message);
    }
    
    /**
     * Cleanup method to prevent memory leaks.
     */
    public void dispose() {
        // Remove listener to prevent memory leaks
        // In a real implementation, you'd track the listener reference
    }
}
/*