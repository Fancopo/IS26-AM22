package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * End-of-round cleanup phase: discards spent cards, shifts the rows, refills the
 * upper row (advancing the Era if a higher-Era card surfaces), and recomputes
 * the turn order from the turn-order tile. It then either ends the game after
 * round 10 ({@link EndGameState}) or starts the next round
 * ({@link TotemPlacementState}).
 */
public class RoundUpdateState implements GameState {

    /**
     * Performs the cleanup, refill, Era handling and turn-order update, then
     * transitions to the next phase.
     *
     * @param game the game being driven
     */
    @Override
    public void updateRound(Game game) {
        game.getBoard().clearLowerRow();
        game.getBoard().shiftUpToLow();
        Era eraAfterRefill = game.getBoard().refillUpperRow(game.getTribeDeck(), game.getCurrentEra());

        if (eraAfterRefill != game.getCurrentEra()) {
            game.setCurrentEra(eraAfterRefill);
            game.handleEraChange();
        }

        List<Totem> newOrder = game.getBoard().getTurnOrderTile().getTurnOrder();
        List<Player> reorderedPlayers = new ArrayList<>();
        for (Totem t : newOrder) {
            reorderedPlayers.add(t.getOwner());
        }

        // Update the players list safely
        game.getPlayers().clear();
        game.getPlayers().addAll(reorderedPlayers);
        game.setActivePlayer(game.getPlayers().getFirst());

        if (game.getCurrentRound() == 10) {
            game.setState(new EndGameState());
            game.determineWinner();
        } else {
            game.setCurrentRound(game.getCurrentRound() + 1);
            game.setState(new TotemPlacementState());
        }
    }

    @Override
    public String getPhaseName() { return "End of Round / Cleanup"; }
}
