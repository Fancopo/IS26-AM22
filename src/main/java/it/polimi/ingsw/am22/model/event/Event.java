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

    @Override
    public String cardCategory() { return "EVENT"; }

    @Override
    public String cardDetailType() { return String.valueOf(eventType); }
    public void applyEvent(List<Player> players, String id){}


    @Override
    public void onRoundEndTrigger(Game game){
        this.effect.applyEvent(game.getPlayers(), this.getId());
    }

    @Override
    public int getTriggerPriority(){
        return this.eventType == EventType.SUSTENANCE ? 1 : 0;
    }

    @Override
    public void addToTribe(Player player, Tribe tribe) {
        throw new UnsupportedOperationException("An event cannot be added to the tribe");
    }
}



