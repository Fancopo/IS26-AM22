package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

/**
 * Phase in which each player, in offer-track order, resolves the action of the
 * tile their totem sits on: gain food and/or take cards. Card picks use a
 * transactional "validate-then-commit" approach so an invalid selection never
 * leaves the game half-mutated. After acting the totem returns to the turn-order
 * tile; when the last player has acted the phase either grants an end-of-round
 * bonus pick ({@link BonusCardSelectionState}) or proceeds to
 * {@link EventResolutionState}.
 */
public class ActionResolutionState implements GameState {
    /**
     * Resolves the active player's action for the tile their totem occupies.
     * If the player is on a food-only tile, {@code selectedCards} is empty.
     *
     * @param game          the game being driven
     * @param player        the player acting
     * @param selectedCards the cards the player chose (empty for a food-only tile)
     * @throws IllegalStateException    if the player is not on the offer track,
     *                                  or cannot afford the selected buildings
     * @throws IllegalArgumentException if the selection violates the tile's
     *                                  constraints or includes an unpickable card
     */
    @Override
    public void pickCards(Game game, Player player, List<Card> selectedCards) {
        OfferTile currentTile = game.getBoard().getOfferTrack().stream()
                .filter(t -> t.getOccupiedBy() == player.getTotem())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Player is not on the offer track!"));

        // 1. FOOD HANDLING (e.g. tile A)
        if (currentTile.getFoodReward() > 0 && selectedCards.isEmpty()) {
            player.addFood(currentTile.getFoodReward());
        }
        // 2. CARD HANDLING: transactional "validate-then-commit" pattern.
        //    Every check that may fail (tile constraints, unpickable cards such as Events,
        //    insufficient food for buildings) must happen BEFORE any mutation on
        //    player/tribe/board. Otherwise a mixed valid+invalid selection would leave the
        //    valid card already added to the tribe, and a subsequent re-selection would
        //    duplicate it.
        else {
            // --- PHASE 1: VALIDATION (no mutation allowed here) ---

            // 1a. Per-card polymorphic validation: each card declares itself whether it is
            //     pickable (e.g. Event throws, TribeCharacter/Building accept). This runs
            //     BEFORE the count check: this way, when the user selects an unpickable
            //     card they immediately get the specific reason ("event card cannot be
            //     picked") instead of the generic count error.
            for (Card card : selectedCards) {
                card.validatePickable();
            }

            // 1b. Tile constraints (number of upper/lower cards).
            //     Cards fall into three categories:
            //       - unpickable (Event)             -> never selectable
            //       - optional purchases (Building)  -> the player decides whether to buy
            //                                           them, so they must never be forced
            //                                           into a selection just to satisfy the
            //                                           tile's count requirement
            //       - mandatory (Character)          -> must be taken until the tile
            //                                           requirement is filled
            //     From this, for each row we derive a valid [min, max] range of cards to take:
            //       min = min(tile.required, mandatory_available_count)   -> "at least these"
            //       max = min(tile.required, pickable_available_count)    -> "no more than this"
            //     The player can then choose how many buildings to add between min and max.
            int upperSelected = (int) selectedCards.stream().filter(c -> game.getBoard().getUpperRow().contains(c)).count();
            int lowerSelected = (int) selectedCards.stream().filter(c -> game.getBoard().getLowerRow().contains(c)).count();
            int[] upperRange = computeRequiredRange(game.getBoard().getUpperRow(), currentTile.getUpperCardsToTake());
            int[] lowerRange = computeRequiredRange(game.getBoard().getLowerRow(), currentTile.getLowerCardsToTake());
            if (upperSelected < upperRange[0] || upperSelected > upperRange[1]
                    || lowerSelected < lowerRange[0] || lowerSelected > lowerRange[1]) {
                throw new IllegalArgumentException(
                        "Invalid card selection for the current tile! Required: "
                                + describeRange(upperRange) + " upper, "
                                + describeRange(lowerRange) + " lower"
                                + ((upperRange[1] == 0 && lowerRange[1] == 0)
                                        ? " (no pickable card available: use `pick` with no arguments to pass)"
                                        : ""));
            }

            // 1c. Validate food by simulating the pick IN THE ORDER the player
            //     specified. The order is semantically meaningful because:
            //       - a Builder picked before a Building applies its discount to
            //         that Building (and the following ones);
            //       - a Hunter* picked before a Building feeds the player before
            //         they must pay for the Building;
            //       - a Hunter*'s food bonus depends on the number of Hunters
            //         already in the tribe AT THE MOMENT it is added
            //         (see Hunter.onAddedToTribe), so picking HUNTER -> HUNTER*
            //         yields more food than HUNTER* -> HUNTER.
            //     Each card declares its own effect via Card.applyPickEffect, so
            //     Building/Builder/Hunter are polymorphic dispatches (no instanceof)
            //     and Artist/Inventor/Collector/Shaman inherit the default no-op
            //     (their contribution is at end-game or in specific events, so it
            //     must not be simulated here).
            PickSimulation sim = new PickSimulation(
                    player.getFood(),
                    player.getTribe().getBuilderDiscount(),
                    player.getTribe().countCharacters(CharacterType.HUNTER));
            for (Card card : selectedCards) {
                card.applyPickEffect(sim);
            }

            // --- PHASE 2: COMMIT (validations passed: now we mutate) ---
            // Same order as the simulation: each Building pays the tribe's
            // *current* discount (which grows as Builders are added), and each
            // addCard triggers onAddedToTribe (Hunter* food). No total upfront
            // payFood: with a total upfront, picking [Hunter*, Building] could make
            // payFood fail for temporarily insufficient food, even though the
            // sequence is valid.
            for (Card card : selectedCards) {
                card.payPickCost(player);
                player.getTribe().addCard(player, card);
            }
            game.getBoard().getUpperRow().removeAll(selectedCards);
            game.getBoard().getLowerRow().removeAll(selectedCards);
        }

        // 3. MOVE TOTEM TO TURN-ORDER TRACK
        Slot nextSlot = game.getBoard().getTurnOrderTile().getFirstAvailableSlot();
        player.getTotem().moveToTurnOrder(nextSlot);

        // Turn-order bonus/malus
        if (nextSlot.getFoodBonus() > 0) {
            // 1. The player gets the slot's base food
            player.addFood(nextSlot.getFoodBonus());

            // 2. THE TRIGGER: wake up all of the player's buildings.
            if (player.getTribe() != null) {
                for (Building b : player.getTribe().getBuildings()) {
                    b.applyOnFoodSlotPlaced(player);
                }
            }
        }
        if (nextSlot.isLastSpace()) {
            if (player.getFood() >= 1) {
                player.addFood(-1);
            } else {
                player.addPP(-2);
            }
        }

        // ==========================================
        // END-OF-PHASE CHECK AND EXTRA BONUS DRAW
        // ==========================================
        if (game.getBoard().getTurnOrderTile().getOccupiedSlotsCount() == game.getPlayers().size()) {

            // All totems are back. Check the `extraBuyAtRoundEnd` flag.
            Player bonusPlayer = null;
            for (Player p : game.getPlayers()) {
                if (p.hasExtraBuyAtRoundEnd()) {
                    bonusPlayer = p;
                    break;
                }
            }

            if (bonusPlayer != null) {
                // PLAYER HAS THE BUILDING: pause and switch to the bonus state
                game.setActivePlayer(bonusPlayer);
                game.setState(new BonusCardSelectionState());
            } else {
                // NO BONUS: proceed normally with the events
                game.setState(new EventResolutionState());
                game.resolveEvents();
            }

        } else {
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
        }
    }

    @Override
    public String getPhaseName() { return "Action Resolution"; }

    /**
     * [min, max] range of cards to take from a row, given the tile requirement:
     *   min = how many mandatory cards (pickable && !optional) are present, capped at the requirement
     *   max = how many pickable cards in total are present, capped at the requirement
     * Buildings (optional) sit between min and max: the player chooses whether to buy them.
     */
    private int[] computeRequiredRange(List<Card> row, int tileRequirement) {
        long mandatoryAvailable = row.stream().filter(c -> c.isPickable() && !c.isOptionalPurchase()).count();
        long pickableAvailable = row.stream().filter(Card::isPickable).count();
        int min = (int) Math.min(tileRequirement, mandatoryAvailable);
        int max = (int) Math.min(tileRequirement, pickableAvailable);
        return new int[] { min, max };
    }

    // Renders a [min, max] range as "n" when min == max, otherwise "min-max".
    private String describeRange(int[] range) {
        return range[0] == range[1] ? Integer.toString(range[0]) : range[0] + "-" + range[1];
    }
}
