package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.GameState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventResolutionState implements GameState {

    @Override
    public void resolveEvents(Game game) {
        List<Card> cardsToTrigger = new ArrayList<>(game.getBoard().getLowerRow());

        // Rulebook: at the end of the last round, also resolve Events still
        // visible in the upper row (e.g. the two Final Event cards drawn into
        // the upper row during the previous refill).
        if (game.getCurrentRound() == 10 || game.getTribeDeck().isEmpty()) {
            for (Card c : game.getBoard().getUpperRow()) {
                if (c.isEvent()) {
                    cardsToTrigger.add(c);
                }
            }
        }

        cardsToTrigger.sort(Comparator.comparingInt(Card::getTriggerPriority));

        for (Card c : cardsToTrigger) {
            c.onRoundEndTrigger(game);
        }

        game.setState(new RoundUpdateState());
        game.updateRound();
    }

    @Override
    public String getPhaseName() { return "Event Resolution"; }
}