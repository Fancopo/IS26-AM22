package it.polimi.ingsw.am22;

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
        game.getPlayers().clear();
        for (Totem t : newOrder) {
            game.getPlayers().add(t.getOwner());
        }
        game.setActivePlayer(game.getPlayers().get(0));

        if (game.getCurrentRound() == 10) {
            game.setState(new EndGameState());
            game.determineWinner();
        } else {
            game.setCurrentRound(game.getCurrentRound() + 1);
            game.setState(new TotemPlacementState());
        }

        game.notifyObservers();
    }

    @Override
    public String getPhaseName() { return "Fine Round / Pulizia"; }
}