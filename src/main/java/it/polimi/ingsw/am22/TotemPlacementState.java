package it.polimi.ingsw.am22;

public class TotemPlacementState implements GameState {

    @Override
    public void placeTotemOnOffer(Game game, Player player, OfferTile tile) {
        for (Slot slot : game.getBoard().getTurnOrderTile().getSlots()) {
            if (slot.getOccupiedBy() == player.getTotem()) {
                slot.removeTotem();
                break;
            }
        }
        tile.placeTotem(player.getTotem());

        if (game.getBoard().getTotemsOnOffersCount() == game.getPlayers().size()) {
            // Transizione di Stato!
            game.setState(new ActionResolutionState());
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
        } else {
            int currentIndex = game.getPlayers().indexOf(player);
            game.setActivePlayer(game.getPlayers().get(currentIndex + 1));
        }

        //game.notifyObservers();
    }

    @Override
    public String getPhaseName() { return "Piazzamento Totem"; }
}