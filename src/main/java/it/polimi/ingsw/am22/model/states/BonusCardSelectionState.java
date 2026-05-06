package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.Player;

/**
 * Temporary state activated ONLY if a player owns
 * the bonus-draw building at the end of the action phase.
 */
public class BonusCardSelectionState implements GameState {

    @Override
    public void pickBonusCard(Game game, Player player, Card bonusCard) {
        // 1. Validation: the card MUST be in the upper row
        if (!game.getBoard().getUpperRow().contains(bonusCard)) {
            throw new IllegalArgumentException("Bonus card must be picked from the upper row!");
        }

        // 2. Assign the card (free bonus, no food check)
        player.getTribe().addCard(player, bonusCard);
        game.getBoard().getUpperRow().remove(bonusCard);

        // 3. Automatic transition: bonus taken, proceed with Events
        game.setState(new EventResolutionState());
        game.resolveEvents();
    }

    @Override
    public String getPhaseName() { return "Bonus Card Selection"; }
}