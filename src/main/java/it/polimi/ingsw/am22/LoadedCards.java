package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.event.Event;

import java.util.List;

public class LoadedCards {
    private final List<TribeCharacter> tribeCharacters;
    private final List<Event> events;
    private final List<Event> finalEvents;
    private final List<Building> buildings;

    public LoadedCards(List<TribeCharacter> tribeCharacters,
                       List<Event> events,
                       List<Event> finalEvents,
                       List<Building> buildings) {
        this.tribeCharacters = tribeCharacters;
        this.events = events;
        this.finalEvents = finalEvents;
        this.buildings = buildings;
    }

    public List<TribeCharacter> getTribeCharacters() {
        return tribeCharacters;
    }

    public List<Event> getEvents() {
        return events;
    }

    public List<Event> getFinalEvents() {
        return finalEvents;
    }

    public List<Building> getBuildings() {
        return buildings;
    }
}