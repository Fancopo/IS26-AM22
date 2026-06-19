package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;
import it.polimi.ingsw.am22.model.event.EventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoadedCardsTest {

    @Test
    void gettersReturnTheProvidedLists() {
        List<TribeCharacter> characters = List.of(
                new TribeCharacter("c1", Era.I, 1, CharacterType.HUNTER, null));
        List<Event> events = List.of(
                new Event("e1", Era.I, 1, EventType.HUNTING, null));
        List<Event> finalEvents = List.of(
                new Event("fe1", Era.III, 1, EventType.SUSTENANCE, null));
        List<Building> buildings = List.of(
                new Building("b1", Era.I, 1, 5, 3, null));

        LoadedCards loaded = new LoadedCards(characters, events, finalEvents, buildings);

        assertSame(characters, loaded.getTribeCharacters());
        assertSame(events, loaded.getEvents());
        assertSame(finalEvents, loaded.getFinalEvents());
        assertSame(buildings, loaded.getBuildings());
    }

    @Test
    void keepsTheFourCategoriesSeparate() {
        LoadedCards loaded = new LoadedCards(List.of(), List.of(), List.of(), List.of());

        assertTrue(loaded.getTribeCharacters().isEmpty());
        assertTrue(loaded.getEvents().isEmpty());
        assertTrue(loaded.getFinalEvents().isEmpty());
        assertTrue(loaded.getBuildings().isEmpty());
    }
}
