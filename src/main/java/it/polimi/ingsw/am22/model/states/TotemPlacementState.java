package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;

public class TotemPlacementState implements GameState {

    @Override
    public void placeTotemOnOffer(Game game, Player player, OfferTile tile) {
        player.getTotem().moveToOffer(tile);

        if (game.getBoard().getTotemsOnOffersCount() == game.getPlayers().size()) {
            // State transition
            game.setState(new ActionResolutionState());
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
        } else {
            int currentIndex = game.getPlayers().indexOf(player);
            game.setActivePlayer(game.getPlayers().get(currentIndex + 1));
        }
    }

    @Override
    public String getPhaseName() { return "Totem Placement"; }
}