package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;

import java.util.List;

/**
 * Immutable container for the cards loaded from the JSON resources, grouped by
 * category: tribe characters, regular events, the two reserved final events,
 * and buildings.
 *
 * @see CardJsonLoader
 */
public class LoadedCards {
    private final List<TribeCharacter> tribeCharacters;
    private final List<Event> events;
    private final List<Event> finalEvents;
    private final List<Building> buildings;

    /**
     * @param tribeCharacters the tribe-character cards
     * @param events          the regular event cards
     * @param finalEvents     the reserved final-event cards
     * @param buildings       the building cards
     */
    public LoadedCards(List<TribeCharacter> tribeCharacters,
                       List<Event> events,
                       List<Event> finalEvents,
                       List<Building> buildings) {
        this.tribeCharacters = tribeCharacters;
        this.events = events;
        this.finalEvents = finalEvents;
        this.buildings = buildings;
    }

    /** @return the tribe-character cards */
    public List<TribeCharacter> getTribeCharacters() {
        return tribeCharacters;
    }

    /** @return the regular event cards */
    public List<Event> getEvents() {
        return events;
    }

    /** @return the reserved final-event cards */
    public List<Event> getFinalEvents() {
        return finalEvents;
    }

    /** @return the building cards */
    public List<Building> getBuildings() {
        return buildings;
    }
}
