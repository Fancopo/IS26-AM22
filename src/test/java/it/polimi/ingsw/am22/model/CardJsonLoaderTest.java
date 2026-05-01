package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.*;
import it.polimi.ingsw.am22.model.event.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardJsonLoaderTest {

    @Test
    void loadTribeCharactersAndEvents_shouldLoadExpectedCounts() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertNotNull(loadedCards);
        assertEquals(84, loadedCards.getTribeCharacters().size());
        assertEquals(10, loadedCards.getEvents().size());
        assertEquals(2, loadedCards.getFinalEvents().size());
    }

    @Test
    void shouldLoadHunterCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "1".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.HUNTER, card.getCharacterType());
        assertInstanceOf(Hunter.class, card);
    }

    @Test
    void shouldLoadBuilderCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "6".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.BUILDER, card.getCharacterType());
        assertInstanceOf(Builder.class, card);
        assertEquals(1, card.getDiscountFood());
        assertEquals(3, card.getPP());
    }

    @Test
    void shouldLoadCollectorCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "10".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.COLLECTOR, card.getCharacterType());
        assertInstanceOf(Collector.class, card);
    }

    @Test
    void shouldLoadArtistCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "14".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.ARTIST, card.getCharacterType());
        assertInstanceOf(Artist.class, card);
    }

    @Test
    void shouldLoadInventorCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "19".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.INVENTOR, card.getCharacterType());
        assertInstanceOf(Inventor.class, card);
        assertEquals(0, card.getIconPerInventor());
    }

    @Test
    void shouldLoadShamanCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter card = loadedCards.getTribeCharacters().stream()
                .filter(c -> "26".equals(c.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(card);
        assertEquals(CharacterType.SHAMAN, card.getCharacterType());
        assertInstanceOf(Shaman.class, card);
        assertEquals(2, card.getNumStars());
    }

    @Test
    void shouldContainAllCharacterTypes() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.HUNTER));
        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.BUILDER));
        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.COLLECTOR));
        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.ARTIST));
        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.INVENTOR));
        assertTrue(loadedCards.getTribeCharacters().stream().anyMatch(c -> c.getCharacterType() == CharacterType.SHAMAN));
    }

    @Test
    void shouldLoadHuntingEventCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        Event event = loadedCards.getEvents().stream()
                .filter(e -> "85".equals(e.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(event);
        assertEquals(EventType.HUNTING, event.getEventType());
        assertInstanceOf(Hunting.class, event);
    }

    @Test
    void shouldLoadSustenanceEventCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        Event event = loadedCards.getEvents().stream()
                .filter(e -> "86".equals(e.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(event);
        assertEquals(EventType.SUSTENANCE, event.getEventType());
        assertInstanceOf(sustenance.class, event);
        assertEquals(1, event.getTriggerPriority());
    }

    @Test
    void shouldLoadShamanicRitualEventCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        Event event = loadedCards.getEvents().stream()
                .filter(e -> "87".equals(e.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(event);
        assertEquals(EventType.SHAMANIC_RITUAL, event.getEventType());
        assertInstanceOf(ShamanicRitual.class, event);
    }

    @Test
    void shouldLoadCavePaintingsEventCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        Event event = loadedCards.getEvents().stream()
                .filter(e -> "88".equals(e.getId()))
                .findFirst()
                .orElse(null);

        assertNotNull(event);
        assertEquals(EventType.CAVE_PAINTING, event.getEventType());
        assertInstanceOf(CavePaintings.class, event);
    }

    @Test
    void shouldContainAllEventTypes() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertTrue(loadedCards.getEvents().stream().anyMatch(e -> e.getEventType() == EventType.HUNTING));
        assertTrue(loadedCards.getEvents().stream().anyMatch(e -> e.getEventType() == EventType.SUSTENANCE));
        assertTrue(loadedCards.getEvents().stream().anyMatch(e -> e.getEventType() == EventType.SHAMANIC_RITUAL));
        assertTrue(loadedCards.getEvents().stream().anyMatch(e -> e.getEventType() == EventType.CAVE_PAINTING));
    }

    @Test
    void otherEventsShouldHavePriorityZero() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        loadedCards.getEvents().stream()
                .filter(e -> e.getEventType() != EventType.SUSTENANCE)
                .forEach(e -> assertEquals(0, e.getTriggerPriority()));
    }

    @Test
    void loadBuildings_shouldLoadAllBuildings() {
        CardJsonLoader loader = new CardJsonLoader();

        List<Building> buildings = loader.loadBuildings("/Building.json");

        assertNotNull(buildings);
        assertEquals(21, buildings.size());

        for (Building building : buildings) {
            assertNotNull(building.getId());
            assertNotNull(building.getEra());
            assertNotNull(building.getEffect());
        }
    }

    @Test
    void loadAllCards_shouldLoadEverything() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadAllCards("/TribeCharacter-Event.json", "/Building.json");

        assertNotNull(loadedCards);
        assertEquals(84, loadedCards.getTribeCharacters().size());
        assertEquals(10, loadedCards.getEvents().size());
        assertEquals(2, loadedCards.getFinalEvents().size());
        assertEquals(21, loadedCards.getBuildings().size());
    }
    @Test
    void loadedListsShouldNotContainNulls() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");
        List<Building> buildings = loader.loadBuildings("/Building.json");

        assertTrue(loadedCards.getTribeCharacters().stream().noneMatch(java.util.Objects::isNull));
        assertTrue(loadedCards.getEvents().stream().noneMatch(java.util.Objects::isNull));
        assertTrue(loadedCards.getFinalEvents().stream().noneMatch(java.util.Objects::isNull));
        assertTrue(buildings.stream().noneMatch(java.util.Objects::isNull));
    }
    @Test
    void idsShouldBeUniqueInsideEachCategory() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");
        List<Building> buildings = loader.loadBuildings("/Building.json");

        List<String> tribeIds = loadedCards.getTribeCharacters().stream().map(TribeCharacter::getId).toList();
        List<String> eventIds = loadedCards.getEvents().stream().map(Event::getId).toList();
        List<String> finalEventIds = loadedCards.getFinalEvents().stream().map(Event::getId).toList();
        List<String> buildingIds = buildings.stream().map(Building::getId).toList();

        assertEquals(tribeIds.size(), new java.util.HashSet<>(tribeIds).size());
        assertEquals(eventIds.size(), new java.util.HashSet<>(eventIds).size());
        assertEquals(finalEventIds.size(), new java.util.HashSet<>(finalEventIds).size());
        assertEquals(buildingIds.size(), new java.util.HashSet<>(buildingIds).size());
    }
    @Test
    void shouldThrowWhenTribeEventFileDoesNotExist() {
        CardJsonLoader loader = new CardJsonLoader();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.loadTribeCharactersAndEvents("/file-che-non-esiste.json"));

        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }
    @Test
    void shouldThrowWhenBuildingFileDoesNotExist() {
        CardJsonLoader loader = new CardJsonLoader();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> loader.loadBuildings("/file-che-non-esiste.json"));

        assertNotNull(ex.getCause());
        assertInstanceOf(IllegalArgumentException.class, ex.getCause());
    }
    @Test
    void shouldContainAllBuildingEffectTypes() {
        CardJsonLoader loader = new CardJsonLoader();
        List<Building> buildings = loader.loadBuildings("/Building.json");

        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.CollectionRewardEffect));
        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.SustenanceDiscountEffect));
        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.ShamanicModifierEffect));
        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.EventYieldBonusEffect));
        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.EndGameScoringEffect));
        assertTrue(buildings.stream().anyMatch(b -> b.getEffect() instanceof it.polimi.ingsw.am22.model.building.TurnPhaseModifierEffect));
    }
    @Test
    void allBuildingsShouldHaveMinPlayersTwo() {
        CardJsonLoader loader = new CardJsonLoader();
        List<Building> buildings = loader.loadBuildings("/Building.json");

        buildings.forEach(b -> assertEquals(2, b.getMinPlayers()));
    }
    @Test
    void shouldSeparateFinalEventsCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();
        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        List<String> normalEventIds = loadedCards.getEvents().stream().map(Event::getId).toList();
        List<String> finalEventIds = loadedCards.getFinalEvents().stream().map(Event::getId).toList();

        assertEquals(2, finalEventIds.size());
        assertTrue(finalEventIds.contains("95"));
        assertTrue(finalEventIds.contains("96"));

        assertFalse(normalEventIds.contains("95"));
        assertFalse(normalEventIds.contains("96"));
    }
}
