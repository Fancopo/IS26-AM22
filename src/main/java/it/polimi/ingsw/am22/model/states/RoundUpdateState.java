package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;

import java.util.ArrayList;
import java.util.List;

public class RoundUpdateState implements GameState {

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

        //game.notifyObservers();
    }

    @Override
    public String getPhaseName() { return "Fine Round / Pulizia"; }
}