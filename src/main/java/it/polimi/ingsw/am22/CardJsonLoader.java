package it.polimi.ingsw.am22;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.Building.*;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.character.*;
import it.polimi.ingsw.am22.event.*;
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
                            tribeCharacters.add(parseTribeCharacter(node));

                    case "event" ->
                            events.add(parseEvent(node));

                    default ->
                            throw new IllegalArgumentException("Tipo non valido in " + resourcePath + ": " + type);
                }
            }

            return new LoadedCards(tribeCharacters, events, new ArrayList<>());

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento di tribeCharacter/event", e);
        }
    }
    private TribeCharacter parseTribeCharacter(JsonNode node) {
        try {
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            CharacterType characterType = CharacterType.valueOf(node.get("characterType").asText());

            JsonNode effectNode = node.get("effect");
            CharacterEffect effect = parseCharacterEffect(characterType, effectNode);

            return new TribeCharacter(id, era, minPlayers, characterType, effect);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel parsing di tribeCharacter", e);
        }
    }

    private CharacterEffect parseCharacterEffect(CharacterType characterType, JsonNode effectNode) {
        try {
            return switch (characterType) {
                case INVENTOR -> mapper.treeToValue(effectNode, Inventor.class);
                case ARTIST -> mapper.treeToValue(effectNode, Artist.class);
                case HUNTER -> mapper.treeToValue(effectNode, Hunter.class);
                case BUILDER -> mapper.treeToValue(effectNode, Builder.class);
                case SHAMAN -> mapper.treeToValue(effectNode, Shaman.class);
                case COLLECTOR -> mapper.treeToValue(effectNode, Collector.class);
            };
        } catch (Exception e) {
            throw new RuntimeException("Errore nel parsing di CharacterEffect: " + characterType, e);
        }
    }

    private Event parseEvent(JsonNode node) {
        try {
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            EventType eventType = EventType.valueOf(node.get("eventType").asText());

            EventEffect effect = parseEventEffect(eventType, era);

            return new Event(id, era, minPlayers, eventType, effect);

        } catch (Exception e) {
            throw new RuntimeException("Errore nel parsing di event", e);
        }
    }

    private EventEffect parseEventEffect(EventType eventType, Era era) {
        return switch (eventType) {
            case HUNTING -> new hunting(era);
            case SHAMANIC_RITUAL -> new ShamanicRitual(era);
            case CAVE_PAINTING -> new CavePaintings(era);
            case SUSTENANCE -> new sustenance(era);
        };
    }
    public List<Building> loadBuildings(String resourcePath) {
        List<Building> buildings = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("File non trovato: " + resourcePath);
            }

            JsonNode root = mapper.readTree(is);

            if (!root.isArray()) {
                throw new IllegalArgumentException("La radice del JSON dei building deve essere un array");
            }

            for (JsonNode node : root) {
                buildings.add(parseBuilding(node));
            }

            return buildings;

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento dei building", e);
        }
    }


    private Building parseBuilding(JsonNode node) {
        String id = node.get("id").asText();
        Era era = Era.valueOf(node.get("era").asText());
        int minPlayers = 2;
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
                    "BuildingEffectType sconosciuto nel building " + buildingId + ": " + effectType
            );
        };
    }

    private CharacterType readNullableCharacterType(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return CharacterType.valueOf(node.asText());
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