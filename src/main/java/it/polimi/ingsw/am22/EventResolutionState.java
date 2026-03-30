package it.polimi.ingsw.am22;

import java.util.ArrayList;
import java.util.List;

public class EventResolutionState implements GameState {

    @Override
    public void resolveEvents(Game game) {
        game.notifyObservers(); // Notifica che siamo entrati negli eventi

        List<Event> activeEvents = new ArrayList<>();

        CardVisitor eventExtractor = new CardVisitor() {
            @Override public void visit(Event event) { activeEvents.add(event); }
            @Override public void visit(Building building) { }
            @Override public void visit(TribeCharacter character) { }
        };

        for (Card c : game.getBoard().getLowerRow()) {
            c.accept(eventExtractor);
        }

        activeEvents.sort((e1, e2) -> {
            if (e1.getEventType() == EventType.SUSTENANCE) return 1;
            if (e2.getEventType() == EventType.SUSTENANCE) return -1;
            return 0;
        });

        for (Event event : activeEvents) {
            event.getEffect().applyEvent(game.getPlayers());
        }

        // Transizione di Stato!
        game.setState(new RoundUpdateState());
        game.updateRound(); // Chiama la pulizia automatica
    }

    @Override
    public String getPhaseName() { return "Risoluzione Eventi"; }
}