package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Slot;

import java.util.Collections;

/**
 * Initial phase of the game. {@link #startMatch(Game)} prepares the turn-order
 * tile and offer track, builds and deals the decks, randomises the player order,
 * seats every totem and hands out the starting food, then advances to
 * {@link TotemPlacementState}.
 */
public class SetUpState implements GameState {

    /**
     * Sets up the match and transitions to the totem-placement phase.
     *
     * @param game the game being set up
     */
    @Override
    public void startMatch(Game game) {
        game.getBoard().getTurnOrderTile().setup(game.getPlayers().size());
        game.getBoard().initTrack(game.getPlayers().size());

        // Builds and shuffles the decks, revealing the Era I buildings.
        game.setupDecks();

        game.getBoard().dealInitialCards(game.getTribeDeck(), game.getPlayers().size());
        Collections.shuffle(game.getPlayers());
        game.setActivePlayer(game.getPlayers().getFirst());

        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player p = game.getPlayers().get(i);
            Slot slot = game.getBoard().getTurnOrderTile().getSlots().get(i);
            p.getTotem().moveToTurnOrder(slot);

            if (i == 0) p.addFood(2);
            else if (i == 1 || i == 2) p.addFood(3);
            else if (i == 3 || i == 4) p.addFood(4);
        }

        // State transition
        game.setState(new TotemPlacementState());
    }

    @Override
    public String getPhaseName() { return "Initial Setup"; }

    @Override
    public boolean isSetupPhase() { return true; }
}
