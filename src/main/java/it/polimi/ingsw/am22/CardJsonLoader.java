package it.polimi.ingsw.am22;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.event.Event;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CardJsonLoader {

    private final ObjectMapper mapper = new ObjectMapper();

    public LoadedCards loadTribeCharactersAndEvents(String resourcePath) {
        List<TribeCharacter> tribeCharacters = new ArrayList<>();
        List<Event> events = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File non trovato: " + resourcePath);
            }

            JsonNode root = mapper.readTree(is);

            if (!root.isArray()) {
                throw new IllegalArgumentException("La radice del JSON deve essere un array");
            }

            for (JsonNode node : root) {
                String type = node.get("type").asText();

                switch (type) {
                    case "tribeCharacter" ->
                            tribeCharacters.add(mapper.treeToValue(node, TribeCharacter.class));

                    case "event" ->
                            events.add(mapper.treeToValue(node, Event.class));

                    default ->
                            throw new IllegalArgumentException("Tipo non valido in " + resourcePath + ": " + type);
                }
            }

            return new LoadedCards(tribeCharacters, events, new ArrayList<>());

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento di tribeCharacter/event", e);
        }
    }

    public List<Building> loadBuildings(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File non trovato: " + resourcePath);
            }

            return mapper.readValue(is, new TypeReference<List<Building>>() {});

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento dei building", e);
        }
    }

    public LoadedCards loadAllCards(String tribeEventPath, String buildingPath) {
        LoadedCards partial = loadTribeCharactersAndEvents(tribeEventPath);
        List<Building> buildings = loadBuildings(buildingPath);

        return new LoadedCards(
                partial.getTribeCharacters(),
                partial.getEvents(),
                buildings
        );
    }
}