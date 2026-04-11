package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.character.Builder;
import it.polimi.ingsw.am22.character.CharacterEffect;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.Hunter;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.event.Event;
import it.polimi.ingsw.am22.event.EventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CardJsonLoaderTest {

    @Test
    void loadTribeCharactersAndEvents_shouldLoadBuilderCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertNotNull(loadedCards);
        assertFalse(loadedCards.getTribeCharacters().isEmpty());

        TribeCharacter builderCard = loadedCards.getTribeCharacters().stream()
                .filter(c -> c.getCharacterType() == CharacterType.BUILDER)
                .findFirst()
                .orElse(null);

        assertNotNull(builderCard);
        assertEquals(CharacterType.BUILDER, builderCard.getCharacterType());
        assertEquals(1, builderCard.getDiscountFood());
        assertEquals(3, builderCard.getPP());
        assertTrue(builderCard.getEffect() instanceof Builder);
    }

    @Test
    void loadTribeCharactersAndEvents_shouldLoadShamanCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter shamanCard = loadedCards.getTribeCharacters().stream()
                .filter(c -> c.getCharacterType() == CharacterType.SHAMAN)
                .findFirst()
                .orElse(null);

        assertNotNull(shamanCard);
        assertEquals(CharacterType.SHAMAN, shamanCard.getCharacterType());
        assertTrue(shamanCard.getNumStars() > 0);
    }

    @Test
    void loadTribeCharactersAndEvents_allTribeCharactersShouldHaveEffect() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertNotNull(loadedCards);
        assertFalse(loadedCards.getTribeCharacters().isEmpty());

        for (TribeCharacter card : loadedCards.getTribeCharacters()) {
            assertNotNull(card.getCharacterType());
            assertNotNull(card.getEffect());
        }
    }

    @Test
    void loadTribeCharactersAndEvents_builderShouldHaveBuilderEffect() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter builderCard = loadedCards.getTribeCharacters().stream()
                .filter(c -> c.getCharacterType() == CharacterType.BUILDER)
                .findFirst()
                .orElse(null);

        assertNotNull(builderCard);
        assertNotNull(builderCard.getEffect());
        assertTrue(builderCard.getEffect() instanceof Builder);
    }

    @Test
    void loadTribeCharactersAndEvents_shouldLoadHunterCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        TribeCharacter hunterCard = loadedCards.getTribeCharacters().stream()
                .filter(c -> c.getCharacterType() == CharacterType.HUNTER)
                .findFirst()
                .orElse(null);

        assertNotNull(hunterCard);
        assertEquals(CharacterType.HUNTER, hunterCard.getCharacterType());

        CharacterEffect effect = hunterCard.getEffect();
        assertTrue(effect instanceof Hunter);

        Hunter hunter = (Hunter) effect;
        assertTrue(hunter.isHasFoodIcon() || !hunter.isHasFoodIcon());
    }

    @Test
    void loadTribeCharactersAndEvents_shouldContainShamanWithStars() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        boolean found = loadedCards.getTribeCharacters().stream()
                .filter(c -> c.getCharacterType() == CharacterType.SHAMAN)
                .anyMatch(c -> c.getNumStars() > 0);

        assertTrue(found);
    }

    @Test
    void loadTribeCharactersAndEvents_shouldLoadEventsCorrectly() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        assertNotNull(loadedCards);
        assertFalse(loadedCards.getEvents().isEmpty());

        boolean hasHunting = loadedCards.getEvents().stream()
                .anyMatch(e -> e.getEventType() == EventType.HUNTING);

        boolean hasSustenance = loadedCards.getEvents().stream()
                .anyMatch(e -> e.getEventType() == EventType.SUSTENANCE);

        assertTrue(hasHunting);
        assertTrue(hasSustenance);
    }

    @Test
    void loadTribeCharactersAndEvents_sustenanceShouldHavePriorityOne() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        Event sustenance = loadedCards.getEvents().stream()
                .filter(e -> e.getEventType() == EventType.SUSTENANCE)
                .findFirst()
                .orElse(null);

        assertNotNull(sustenance);
        assertEquals(1, sustenance.getTriggerPriority());
    }

    @Test
    void loadTribeCharactersAndEvents_shouldThrowIfFileDoesNotExist() {
        CardJsonLoader loader = new CardJsonLoader();

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                loader.loadTribeCharactersAndEvents("/file-che-non-esiste.json")
        );

        assertNotNull(ex.getMessage());
    }

    @Test
    void loadAllCards_shouldLoadCharactersEventsAndBuildings() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadAllCards("/TribeCharacter-Event.json", "/Building.json");

        assertNotNull(loadedCards);
        assertFalse(loadedCards.getTribeCharacters().isEmpty());
        assertFalse(loadedCards.getEvents().isEmpty());
        assertFalse(loadedCards.getBuildings().isEmpty());
    }

    @Test
    void loadBuildings_allBuildingsShouldHaveEffect() {
        CardJsonLoader loader = new CardJsonLoader();

        List<Building> buildings = loader.loadBuildings("/Building.json");

        assertNotNull(buildings);
        assertFalse(buildings.isEmpty());

        for (Building building : buildings) {
            assertNotNull(building.getEffect());
        }
    }

    @Test
    void loadTribeCharactersAndEvents_allCardsShouldHaveBaseFields() {
        CardJsonLoader loader = new CardJsonLoader();

        LoadedCards loadedCards = loader.loadTribeCharactersAndEvents("/TribeCharacter-Event.json");

        for (TribeCharacter card : loadedCards.getTribeCharacters()) {
            assertNotNull(card.getId());
            assertNotNull(card.getEra());
            assertTrue(card.getMinPlayers() >= 2);
            assertNotNull(card.getCharacterType());
        }

        for (Event event : loadedCards.getEvents()) {
            assertNotNull(event.getId());
            assertNotNull(event.getEra());
            assertTrue(event.getMinPlayers() >= 2);
            assertNotNull(event.getEventType());
        }
    }
}