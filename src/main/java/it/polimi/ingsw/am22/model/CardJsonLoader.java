package it.polimi.ingsw.am22.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.model.building.*;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.building.CollectionCondition;
import it.polimi.ingsw.am22.model.character.*;
import it.polimi.ingsw.am22.model.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads cards from JSON files.
 *
 * Responsible for:
 * - loading TribeCharacter and Event
 * - separating final events from regular events
 * - loading Buildings
 * - building a final LoadedCards object containing all cards
 */
public class CardJsonLoader {

    /** ID of the first final event. */
    private static final int FINAL_EVENT_ID_1 = 95;

    /** ID of the second final event. */
    private static final int FINAL_EVENT_ID_2 = 96;

    /** Jackson mapper used to read and parse the JSON. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Loads all TribeCharacters and Events from a JSON file.
     *
     * Events with IDs 95 and 96 are stored separately
     * in the finalEvents list.
     *
     * @param resourcePath path to the JSON resource
     * @return LoadedCards with tribeCharacters, events, finalEvents and an empty buildings list
     */
    public LoadedCards loadTribeCharactersAndEvents(String resourcePath) {
        List<TribeCharacter> tribeCharacters = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<Event> finalEvents = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            // Verify the file actually exists in project resources
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + resourcePath);
            }

            // Read JSON content and parse it into a Jackson node tree
            JsonNode root = mapper.readTree(is);

            // The file root must be an array of cards
            if (!root.isArray()) {
                throw new IllegalArgumentException("JSON root must be an array");
            }

            // Iterate over every card in the JSON file
            for (JsonNode node : root) {
                String type = node.get("type").asText();

                // Decide how to parse based on the card type
                switch (type) {
                    case "tribeCharacter" -> tribeCharacters.add(parseTribeCharacter(node));

                    case "event" -> {
                        Event event = parseEvent(node);

                        // Final events are stored in a separate list
                        if (isFinalEventNode(node)) {
                            finalEvents.add(event);
                        } else {
                            events.add(event);
                        }
                    }

                    // Any unexpected type triggers an error
                    default -> throw new IllegalArgumentException(
                            "Invalid type in " + resourcePath + ": " + type
                    );
                }
            }

            // Return the final object with tribeCharacters, events and finalEvents.
            // The buildings list is still empty here, it is loaded separately.
            return new LoadedCards(tribeCharacters, events, finalEvents, new ArrayList<>());

        } catch (Exception e) {
            // Wrap any error in a RuntimeException with a clearer message
            throw new RuntimeException("Error loading tribeCharacter/event", e);
        }
    }

    /**
     * Checks whether a JSON node represents a final event.
     *
     * @param node card JSON node
     * @return true if the id is 95 or 96, false otherwise
     */
    private boolean isFinalEventNode(JsonNode node) {
        int id = node.get("id").asInt();
        return id == FINAL_EVENT_ID_1 || id == FINAL_EVENT_ID_2;
    }

    /**
     * Converts a JSON node into a concrete TribeCharacter object.
     *
     * Based on the characterType field, builds the correct subclass:
     * Hunter, Builder, Shaman, Inventor, Artist or Collector.
     *
     * @param node JSON node of the tribeCharacter card
     * @return TribeCharacter object
     */
    private TribeCharacter parseTribeCharacter(JsonNode node) {
        try {
            // Read the fields shared by all character cards
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            CharacterType characterType = CharacterType.valueOf(node.get("characterType").asText());

            // Nested node with effect-specific data
            JsonNode effectNode = node.get("effect");

            // Build the concrete subclass based on character type
            return switch (characterType) {
                case HUNTER -> new Hunter(
                        id,
                        era,
                        minPlayers,
                        effectNode.get("hasFoodIcon").asBoolean()
                );

                case BUILDER -> new Builder(
                        id,
                        era,
                        minPlayers,
                        effectNode.get("discountFood").asInt(),
                        effectNode.get("PP").asInt()
                );

                case SHAMAN -> new Shaman(
                        id,
                        era,
                        minPlayers,
                        effectNode.get("numStars").asInt()
                );

                case INVENTOR -> {
                    // The JSON stores the icon as a one-character string ("A", "B", ...),
                    // not as a numeric code point. Using asInt() silently returns 0 and
                    // we'd end up with a NUL char (rendered as a placeholder glyph in
                    // the terminal). Read it as text and take the first character.
                    String iconText = effectNode.get("iconPerInventor").asText();
                    if (iconText == null || iconText.isEmpty()) {
                        throw new IllegalStateException(
                                "Inventor card " + id + " is missing the 'iconPerInventor' field.");
                    }
                    yield new Inventor(id, era, minPlayers, iconText.charAt(0));
                }

                case ARTIST -> new Artist(id, era, minPlayers);
                case COLLECTOR -> new Collector(id, era, minPlayers);
            };

        } catch (Exception e) {
            throw new RuntimeException("Error parsing tribeCharacter", e);
        }
    }

    /**
     * Converts a JSON node into a concrete Event object.
     *
     * Builds the correct subclass based on the eventType field.
     *
     * @param node JSON node of the event card
     * @return Event object
     */
    private Event parseEvent(JsonNode node) {
        try {
            // Read fields shared by all events
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            EventType eventType = EventType.valueOf(node.get("eventType").asText());

            // Build the concrete event subclass
            return switch (eventType) {
                case HUNTING -> new Hunting(
                        id,
                        era,
                        minPlayers,
                        EventType.HUNTING,
                        null
                );

                case SUSTENANCE -> new Sustenance(
                        id,
                        era,
                        minPlayers,
                        EventType.SUSTENANCE,
                        null
                );

                case SHAMANIC_RITUAL -> new ShamanicRitual(
                        id,
                        era,
                        minPlayers,
                        EventType.SHAMANIC_RITUAL,
                        null
                );

                case CAVE_PAINTING -> new CavePaintings(
                        id,
                        era,
                        minPlayers,
                        EventType.CAVE_PAINTING,
                        null
                );
            };

        } catch (Exception e) {
            throw new RuntimeException("Error parsing event", e);
        }
    }

    /**
     * Loads all buildings from a JSON file.
     *
     * @param resourcePath path to the buildings JSON file
     * @return list of loaded buildings
     */
    public List<Building> loadBuildings(String resourcePath) {
        List<Building> buildings = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            // Verify the file exists
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + resourcePath);
            }

            // Read JSON
            JsonNode root = mapper.readTree(is);

            // Root must also be an array here
            if (!root.isArray()) {
                throw new IllegalArgumentException("Buildings JSON root must be an array");
            }

            // Convert each JSON node into a Building
            for (JsonNode node : root) {
                buildings.add(parseBuilding(node));
            }

            return buildings;

        } catch (Exception e) {
            throw new RuntimeException("Error loading buildings", e);
        }
    }

    /**
     * Converts a JSON node into a Building object.
     *
     * @param node JSON node of the building
     * @return Building object
     */
    private Building parseBuilding(JsonNode node) {
        // Read main building fields
        String id = node.get("id").asText();
        Era era = Era.valueOf(node.get("era").asText());
        int minPlayers = 2; // by design choice, buildings always have minPlayers = 2
        int foodPrice = node.get("foodPrice").asInt();
        int finalPP = node.get("finalPP").asInt();

        // Building effect type
        String effectType = node.get("BuildingEffectType").asText();

        // Nested node with effect-specific data
        JsonNode effectNode = node.get("effect");

        // Parse the concrete effect
        BuildingEffect effect = parseBuildingEffect(effectType, effectNode, id);

        // Build the final Building object
        return new Building(id, era, minPlayers, foodPrice, finalPP, effect);
    }

    /**
     * Converts the building effect data into the correct concrete object.
     *
     * @param effectType effect type read from JSON
     * @param effectNode JSON node holding effect data
     * @param buildingId building id, useful for error messages
     * @return concrete BuildingEffect object
     */
    private BuildingEffect parseBuildingEffect(String effectType, JsonNode effectNode, String buildingId) {
        return switch (effectType) {
            case "COLLECTION_REWARD_EFFECT" -> new CollectionRewardEffect(
                    CollectionCondition.valueOf(effectNode.get("conditionType").asText()),
                    effectNode.get("foodReward").asInt()
            );

            case "SUSTENANCE_DISCOUNT_EFFECT" -> new SustenanceDiscountEffect(
                    CharacterType.valueOf(effectNode.get("targetCharacterType").asText())
            );

            case "SHAMANIC_MODIFIER_EFFECT" -> new ShamanicModifierEffect(
                    effectNode.get("extraIcons").asInt(),
                    effectNode.get("preventPPLoss").asBoolean(),
                    effectNode.get("doubleWinPP").asBoolean()
            );

            case "EVENT_YIELD_BONUS_EFFECT" -> new EventYieldBonusEffect(
                    EventType.valueOf(effectNode.get("targetEventType").asText()),
                    effectNode.get("bonusFood").asInt(),
                    effectNode.get("bonusPP").asInt()
            );

            case "ENDGAME_SCORING_EFFECT" -> new EndGameScoringEffect(
                    effectNode.get("flatPP").asInt(),
                    effectNode.get("pointsPerSet").asInt(),
                    readNullableCharacterType(effectNode.get("targetCharacterType")),
                    effectNode.get("multiplierPP").asInt(),
                    effectNode.get("doubleBuilderPP").asBoolean()
            );

            case "TURN_PHASE_MODIFIER_EFFECT" -> new TurnPhaseModifierEffect(
                    effectNode.get("extraFoodOnTurnOrderBonus").asBoolean(),
                    effectNode.get("extraBuyAtRoundEnd").asBoolean()
            );

            default -> throw new IllegalArgumentException(
                    "Unknown BuildingEffectType in building " + buildingId + ": " + effectType
            );
        };
    }

    /**
     * Reads a CharacterType that may be null.
     *
     * Used when the targetCharacterType field in the JSON
     * may be absent or set to null.
     *
     * @param node JSON node to read
     * @return matching CharacterType, or null
     */
    private CharacterType readNullableCharacterType(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return CharacterType.valueOf(node.asText());
    }

    /**
     * Loads all game cards:
     * - tribeCharacters
     * - events
     * - finalEvents
     * - buildings
     *
     * @param tribeEventPath path to the JSON with tribeCharacter and event
     * @param buildingPath path to the JSON with the buildings
     * @return complete LoadedCards
     */
    public LoadedCards loadAllCards(String tribeEventPath, String buildingPath) {
        // First load tribeCharacter, event and finalEvent
        LoadedCards partial = loadTribeCharactersAndEvents(tribeEventPath);

        // Then load the buildings
        List<Building> buildings = loadBuildings(buildingPath);

        // Build the final object with all cards
        return new LoadedCards(
                partial.getTribeCharacters(),
                partial.getEvents(),
                partial.getFinalEvents(),
                buildings
        );
    }
}
