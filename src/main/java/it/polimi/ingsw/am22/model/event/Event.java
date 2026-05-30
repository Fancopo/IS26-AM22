package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.*;

import java.util.List;

public class Event extends Card {
    private EventType eventType;
    private EventEffect effect;

    public Event(String id, Era era, int minPlayers, EventType eventType, EventEffect effect) {
        super(id, era, minPlayers);
        this.eventType = eventType;
        this.effect = effect;
    }

    public EventType getEventType() { return eventType; }

    /** Lets subclasses register themselves as their own effect after the super() call. */
    protected void setEffect(EventEffect effect) {
        this.effect = effect;
    }

    @Override
    public boolean isEvent() { return true; }

    @Override
    public String cardCategory() { return "EVENT"; }

    @Override
    public String cardDetailType() { return String.valueOf(eventType); }
    public void applyEvent(List<Player> players, String id) {}

    @Override
    public void onRoundEndTrigger(Game game) {
        effect.applyEvent(game.getPlayers(), getId());
    }

    @Override
    public int getTriggerPriority() {
        // Sustenance resolves after every other event of the round.
        return eventType == EventType.SUSTENANCE ? 1 : 0;
    }

    @Override
    public void addToTribe(Player player, Tribe tribe) {
        throw new UnsupportedOperationException("An event cannot be added to the tribe");
    }

    @Override
    public void validatePickable() {
        throw new IllegalArgumentException("An event card cannot be picked into the tribe");
    }

    @Override
    public boolean isPickable() { return false; }
}



