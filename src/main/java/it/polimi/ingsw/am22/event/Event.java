package it.polimi.ingsw.am22.event;
import it.polimi.ingsw.am22.Era;

import java.util.ArrayList;
import java.util.List;

public abstract class Event extends Card {
    protected EventType eventType;

    public Event(Era era, int minPlayers, EventType eventType) {
        super(era, minPlayers);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
    public abstract void applyEvent(List<Player> players, char id);
}



