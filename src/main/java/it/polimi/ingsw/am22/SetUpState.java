package it.polimi.ingsw.am22;

import java.util.Collections;

public class SetUpState implements GameState {

    @Override
    public void startMatch(Game game) {
        game.getBoard().getTurnOrderTile().setup(game.getPlayers().size());
        game.getBoard().initTrack(game.getPlayers().size());

        // Chiama il metodo package-private di Game
        game.setupDecks();

        game.getBoard().dealInitialCards(game.getTribeDeck(), game.getPlayers().size());
        Collections.shuffle(game.getPlayers());
        game.setActivePlayer(game.getPlayers().getFirst());

        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player p = game.getPlayers().get(i);
            Slot slot = game.getBoard().getTurnOrderTile().getSlots().get(i);
            p.getTotem().moveToTurnOrder(slot);

            if (i == 0) p.addFood(2);
            else if (i == 1 || i == 2) p.addFood(3);
            else if (i == 3 || i == 4) p.addFood(4);
        }

        // Transizione di Stato!
        game.setState(new TotemPlacementState());
        //game.notifyObservers();
    }

    @Override
    public String getPhaseName() { return "Setup Iniziale"; }
}