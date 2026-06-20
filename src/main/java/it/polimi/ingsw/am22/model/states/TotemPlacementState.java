package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;

/**
 * Phase in which each player, in turn, places their totem on an offer tile. Once
 * every player has placed, the game moves to {@link ActionResolutionState} and
 * the player on the leftmost occupied tile acts first.
 */
public class TotemPlacementState implements GameState {

    /**
     * Places the player's totem on the chosen tile, then either passes the turn
     * to the next player or, if everyone has placed, starts the action phase.
     *
     * @param game   the game being driven
     * @param player the player placing their totem
     * @param tile   the chosen offer tile
     */
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
