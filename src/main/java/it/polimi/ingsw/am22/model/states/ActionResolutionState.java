package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.Builder;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.Hunter;

import java.util.List;

public class ActionResolutionState implements GameState {
    // The only method the Controller will invoke.
    // If the player is on tile A (food only), selectedCards will be an empty list.
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

            // 1c. Verifica del food simulando la pick NELL'ORDINE in cui il
            //     giocatore l'ha specificata. L'ordine è semanticamente
            //     significativo perché:
            //       - un Builder pescato prima di un Building applica il suo
            //         sconto a quel Building (e a quelli successivi);
            //       - un Hunter* pescato prima di un Building dà food al
            //         giocatore prima che debba pagare il Building;
            //       - il bonus food di un Hunter* dipende dal numero di
            //         Hunter già nella tribù AL MOMENTO in cui viene aggiunto
            //         (cfr. Hunter.onAddedToTribe), quindi pescare HUNTER ->
            //         HUNTER* dà più food di HUNTER* -> HUNTER.
            //     Il calcolo "atomico" precedente leggeva builderDiscount una
            //     sola volta dalla tribù iniziale e sommava i costi di tutti
            //     i Building, ignorando completamente questi effetti.
            int simulatedFood = player.getFood();
            int simulatedDiscount = player.getTribe().getBuilderDiscount();
            int simulatedHunterCount = player.getTribe().countCharacters(CharacterType.HUNTER);

            for (Card card : selectedCards) {
                if (card instanceof Building) {
                    int actualCost = Math.max(0, card.getFoodCost() - simulatedDiscount);
                    if (simulatedFood < actualCost) {
                        throw new IllegalStateException(
                                "Insufficient food to purchase the selected cards.");
                    }
                    simulatedFood -= actualCost;
                } else if (card instanceof Builder builderCard) {
                    simulatedDiscount += builderCard.getDiscountFood();
                } else if (card instanceof Hunter hunterCard) {
                    // Hunter.onAddedToTribe legge la tribù DOPO addCharacter,
                    // quindi il count include il Hunter appena aggiunto.
                    simulatedHunterCount++;
                    if (hunterCard.hasFoodIcon()) {
                        simulatedFood += simulatedHunterCount;
                    }
                }
                // Artist, Inventor, Collector, Shaman: nessun effetto immediato
                // su food / discount (il loro contributo è in fase di EndGame
                // o durante eventi specifici), quindi non serve simularli qui.
            }

            // --- PHASE 2: COMMIT (validazioni passate: ora si muta) ---
            // Stesso ordine della simulazione: ogni Building paga lo sconto
            // *attuale* della tribù (che cresce man mano che si aggiungono
            // Builder), e ogni addCard scatena onAddedToTribe (Hunter* food).
            // Niente payFood "totale" upfront: con un upfront totale, picking
            // [Hunter*, Building] potrebbe far fallire payFood per food
            // temporaneamente insufficiente, anche se la sequenza è valida.
            for (Card card : selectedCards) {
                if (card instanceof Building) {
                    int discount = player.getTribe().getBuilderDiscount();
                    int cost = Math.max(0, card.getFoodCost() - discount);
                    player.payFood(cost);
                }
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

    private String describeRange(int[] range) {
        return range[0] == range[1] ? Integer.toString(range[0]) : range[0] + "-" + range[1];
    }
}
