package it.polimi.ingsw.am22.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.model.building.*;
import it.polimi.ingsw.am22.model.character.*;
import it.polimi.ingsw.am22.model.event.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads cards (TribeCharacters, Events, Buildings) from JSON resources.
 * Events with the two reserved IDs are stored separately as "final events".
 */
public class CardJsonLoader {

    private static final int FINAL_EVENT_ID_1 = 95;
    private static final int FINAL_EVENT_ID_2 = 96;

    private final ObjectMapper mapper = new ObjectMapper();

    public LoadedCards loadTribeCharactersAndEvents(String resourcePath) {
        List<TribeCharacter> tribeCharacters = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<Event> finalEvents = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + resourcePath);
            }

            JsonNode root = mapper.readTree(is);
            if (!root.isArray()) {
                throw new IllegalArgumentException("JSON root must be an array");
            }

            for (JsonNode node : root) {
                String type = node.get("type").asText();
                switch (type) {
                    case "tribeCharacter" -> tribeCharacters.add(parseTribeCharacter(node));
                    case "event" -> {
                        Event event = parseEvent(node);
                        if (isFinalEventNode(node)) {
                            finalEvents.add(event);
                        } else {
                            events.add(event);
                        }
                    }
                    default -> throw new IllegalArgumentException(
                            "Invalid type in " + resourcePath + ": " + type
                    );
                }
            }

            return new LoadedCards(tribeCharacters, events, finalEvents, new ArrayList<>());

        } catch (Exception e) {
            throw new RuntimeException("Error loading tribeCharacter/event", e);
        }
    }

    private boolean isFinalEventNode(JsonNode node) {
        int id = node.get("id").asInt();
        return id == FINAL_EVENT_ID_1 || id == FINAL_EVENT_ID_2;
    }

    private TribeCharacter parseTribeCharacter(JsonNode node) {
        try {
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            CharacterType characterType = CharacterType.valueOf(node.get("characterType").asText());
            JsonNode effectNode = node.get("effect");

            return switch (characterType) {
                case HUNTER -> new Hunter(id, era, minPlayers,
                        effectNode.get("hasFoodIcon").asBoolean());

                case BUILDER -> new Builder(id, era, minPlayers,
                        effectNode.get("discountFood").asInt(),
                        effectNode.get("PP").asInt());

                case SHAMAN -> new Shaman(id, era, minPlayers,
                        effectNode.get("numStars").asInt());

                case INVENTOR -> {
                    // JSON stores the icon as a one-character string ("A", "B", ...). asInt() would
                    // silently return 0 (NUL char), so read it as text and take the first character.
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

    private Event parseEvent(JsonNode node) {
        try {
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            EventType eventType = EventType.valueOf(node.get("eventType").asText());

            return switch (eventType) {
                case HUNTING -> new Hunting(id, era, minPlayers, null);
                case SUSTENANCE -> new Sustenance(id, era, minPlayers, null);
                case SHAMANIC_RITUAL -> new ShamanicRitual(id, era, minPlayers, null);
                case CAVE_PAINTING -> new CavePaintings(id, era, minPlayers, null);
            };

        } catch (Exception e) {
            throw new RuntimeException("Error parsing event", e);
        }
    }

    public List<Building> loadBuildings(String resourcePath) {
        List<Building> buildings = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + resourcePath);
            }
            JsonNode root = mapper.readTree(is);
            if (!root.isArray()) {
                throw new IllegalArgumentException("Buildings JSON root must be an array");
            }
            for (JsonNode node : root) {
                buildings.add(parseBuilding(node));
            }
            return buildings;
        } catch (Exception e) {
            throw new RuntimeException("Error loading buildings", e);
        }
    }

    private Building parseBuilding(JsonNode node) {
        String id = node.get("id").asText();
        Era era = Era.valueOf(node.get("era").asText());
        int minPlayers = 2; // by design, buildings ignore this rule
        int foodPrice = node.get("foodPrice").asInt();
        int finalPP = node.get("finalPP").asInt();
        String effectType = node.get("BuildingEffectType").asText();
        JsonNode effectNode = node.get("effect");

        BuildingEffect effect = parseBuildingEffect(effectType, effectNode, id);
        return new Building(id, era, minPlayers, foodPrice, finalPP, effect);
    }

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

    private CharacterType readNullableCharacterType(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return CharacterType.valueOf(node.asText());
    }

    public LoadedCards loadAllCards(String tribeEventPath, String buildingPath) {
        LoadedCards partial = loadTribeCharactersAndEvents(tribeEventPath);
        List<Building> buildings = loadBuildings(buildingPath);
        return new LoadedCards(
                partial.getTribeCharacters(),
                partial.getEvents(),
                partial.getFinalEvents(),
                buildings
        );
    }
}
