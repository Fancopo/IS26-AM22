package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;
import it.polimi.ingsw.am22.model.PickSimulation;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.CharacterType;

/**
 * Temporary phase, activated only when a player owns the bonus-draw building at
 * the end of the action phase. That player takes one extra card from the upper
 * row (still paying its food cost if it is a Building), after which the round
 * proceeds to {@link EventResolutionState}.
 */
public class BonusCardSelectionState implements GameState {

    /**
     * Resolves the bonus pick: validate the chosen card, charge its cost and add
     * it to the player's tribe, then continue with event resolution.
     *
     * @param game      the game being driven
     * @param player    the player taking the bonus card
     * @param bonusCard the card chosen from the upper row
     * @throws IllegalArgumentException if the card is not in the upper row or is unpickable
     * @throws IllegalStateException    if the player cannot afford the card
     */
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
