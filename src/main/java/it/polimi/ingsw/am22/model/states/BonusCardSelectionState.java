package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.PickSimulation;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.CharacterType;

/**
 * Temporary state activated ONLY if a player owns
 * the bonus-draw building at the end of the action phase.
 */
public class BonusCardSelectionState implements GameState {

    @Override
    public void pickBonusCard(Game game, Player player, Card bonusCard) {
        // 1. Validation: the card MUST be in the upper row and pickable
        if (!game.getBoard().getUpperRow().contains(bonusCard)) {
            throw new IllegalArgumentException("Bonus card must be picked from the upper row!");
        }
        bonusCard.validatePickable();

        // 2. Validate the food cost via the same simulation used in the action
        //    phase: a Building bonus pick still costs food (discounted by the
        //    tribe's Builder count). Characters / other no-cost cards are no-ops.
        PickSimulation sim = new PickSimulation(
                player.getFood(),
                player.getTribe().getBuilderDiscount(),
                player.getTribe().countCharacters(CharacterType.HUNTER));
        bonusCard.applyPickEffect(sim);

        // 3. Commit: pay the cost (if any) and add the card to the tribe
        bonusCard.payPickCost(player);
        player.getTribe().addCard(player, bonusCard);
        game.getBoard().getUpperRow().remove(bonusCard);

        // 4. Automatic transition: bonus taken, proceed with Events
        game.setState(new EventResolutionState());
        game.resolveEvents();
    }

    @Override
    public String getPhaseName() { return "Bonus Card Selection"; }
}