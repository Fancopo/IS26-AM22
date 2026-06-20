package it.polimi.ingsw.am22.model;

/**
 * Observer notified whenever the observed {@link Game}'s state changes.
 * Implemented by the runtime view/network wiring (e.g. the server's virtual
 * view) so it can refresh after every model mutation.
 */
public interface GameObserver {
    /**
     * Invoked after any change to the game state.
     *
     * @param game the game whose state has just changed
     */
    void gameStatusChanged(Game game);
}
