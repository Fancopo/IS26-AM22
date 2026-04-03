package it.polimi.ingsw.am22;

import java.util.List;

public class LoadedCards {
    private final List<TribeCharacter> tribeCharacters;
    private final List<Event> events;
    private final List<Building> buildings;

    public LoadedCards(List<TribeCharacter> tribeCharacters, List<Event> events, List<Building> buildings) {
        this.tribeCharacters = tribeCharacters;
        this.events = events;
        this.buildings = buildings;
    }

    public List<TribeCharacter> getTribeCharacters() {
        return tribeCharacters;
    }

    public List<Event> getEvents() {
        return events;
    }

    public List<Building> getBuildings() {
        return buildings;
    }
}