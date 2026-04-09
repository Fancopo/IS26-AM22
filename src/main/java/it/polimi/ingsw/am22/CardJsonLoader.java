package it.polimi.ingsw.am22;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.event.Event;
import it.polimi.ingsw.am22.Building.*;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.event.EventType;

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
        String id = node.get("id").asText();   // se il costruttore vuole int, usa asInt()
        Era era = Era.valueOf(node.get("era").asText());
        int minPlayers = node.get("minPlayers").asInt();
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