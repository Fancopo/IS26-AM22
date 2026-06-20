package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.*;

import java.util.List;

/**
 * Base type for end-of-round event cards. Events are never added to a tribe and
 * cannot be picked; instead they fire their {@link EventEffect} when the round
 * ends. Concrete subclasses register themselves as their own effect.
 */
public class Event extends Card {
    private EventType eventType;
    private EventEffect effect;

    /**
     * @param id         the card id
     * @param era        the Era the card belongs to
     * @param minPlayers the minimum player count for this card to be in play
     * @param eventType  the event type
     * @param effect     the event effect (subclasses pass {@code null} and register
     *                   themselves via {@link #setEffect})
     */
    public Event(String id, Era era, int minPlayers, EventType eventType, EventEffect effect) {
        super(id, era, minPlayers);
        this.eventType = eventType;
        this.effect = effect;
    }

    /** @return the event type */
    public EventType getEventType() { return eventType; }

    /**
     * Lets subclasses register themselves as their own effect after the super() call.
     *
     * @param effect the effect to use
     */
    protected void setEffect(EventEffect effect) {
        this.effect = effect;
    }

    @Override
    public boolean isEvent() { return true; }

    @Override
    public String cardCategory() { return "EVENT"; }

    @Override
    public String cardDetailType() { return String.valueOf(eventType); }

    /**
     * Applies this event to the given players. The base class does nothing;
     * concrete events provide the behaviour through their {@link EventEffect}.
     *
     * @param players the players in the game
     * @param id      the id of the resolving event
     */
    public void applyEvent(List<Player> players, String id) {}

    /**
     * Fires the event's effect at round end.
     *
     * @param game the game being resolved
     */
    @Override
    public void onRoundEndTrigger(Game game) {
        effect.applyEvent(game.getPlayers(), getId());
    }

    @Override
    public int getTriggerPriority() {
        // Sustenance resolves after every other event of the round.
        return eventType == EventType.SUSTENANCE ? 1 : 0;
    }

    /**
     * @throws UnsupportedOperationException always — an event cannot be added to a tribe
     */
    @Override
    public void addToTribe(Player player, Tribe tribe) {
        throw new UnsupportedOperationException("An event cannot be added to the tribe");
    }

    /**
     * @throws IllegalArgumentException always — an event cannot be picked into the tribe
     */
    @Override
    public void validatePickable() {
        throw new IllegalArgumentException("An event card cannot be picked into the tribe");
    }

    @Override
    public boolean isPickable() { return false; }
}
