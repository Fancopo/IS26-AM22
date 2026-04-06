package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventResolutionState implements GameState {

    @Override
    public void resolveEvents(Game game) {
       // game.notifyObservers();

        List<Card> cardsToTrigger = new ArrayList<>(game.getBoard().getLowerRow());

        cardsToTrigger.sort(Comparator.comparingInt(Card::getTriggerPriority));

        for (Card c : cardsToTrigger) {
            c.onRoundEndTrigger(game);
        }

        game.setState(new RoundUpdateState());
        game.updateRound();
    }

    @Override
    public String getPhaseName() { return "Risoluzione Eventi"; }
}