package it.polimi.ingsw.am22.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.polimi.ingsw.am22.model.Building.*;
import it.polimi.ingsw.am22.model.Building.BuildingEffect;
import it.polimi.ingsw.am22.model.Building.CollectionCondition;
import it.polimi.ingsw.am22.model.character.*;
import it.polimi.ingsw.am22.model.event.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsabile del caricamento delle carte dal file JSON.
 *
 * Si occupa di:
 * - caricare TribeCharacter ed Event
 * - separare gli eventi finali dagli eventi normali
 * - caricare i Building
 * - costruire un oggetto finale LoadedCards contenente tutte le carte
 */
public class CardJsonLoader {

    /** ID del primo evento finale. */
    private static final int FINAL_EVENT_ID_1 = 95;

    /** ID del secondo evento finale. */
    private static final int FINAL_EVENT_ID_2 = 96;

    /** Mapper Jackson usato per leggere e interpretare il JSON. */
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Carica da un file JSON tutti i TribeCharacter e gli Event.
     *
     * Gli eventi con ID 95 e 96 vengono salvati separatamente
     * nella lista dei finalEvents.
     *
     * @param resourcePath percorso della risorsa JSON
     * @return LoadedCards contenente tribeCharacters, events, finalEvents e lista vuota di buildings
     */
    public LoadedCards loadTribeCharactersAndEvents(String resourcePath) {
        List<TribeCharacter> tribeCharacters = new ArrayList<>();
        List<Event> events = new ArrayList<>();
        List<Event> finalEvents = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            // Controlla che il file esista davvero tra le risorse del progetto
            if (is == null) {
                throw new IllegalArgumentException("File non trovato: " + resourcePath);
            }

            // Legge il contenuto JSON e lo converte in un albero di nodi Jackson
            JsonNode root = mapper.readTree(is);

            // Il file deve avere come radice un array di carte
            if (!root.isArray()) {
                throw new IllegalArgumentException("La radice del JSON deve essere un array");
            }

            // Scorre tutte le carte presenti nel file JSON
            for (JsonNode node : root) {
                String type = node.get("type").asText();

                // In base al tipo della carta decide come interpretarla
                switch (type) {
                    case "tribeCharacter" -> tribeCharacters.add(parseTribeCharacter(node));

                    case "event" -> {
                        Event event = parseEvent(node);

                        // Gli eventi finali vengono salvati in una lista separata
                        if (isFinalEventNode(node)) {
                            finalEvents.add(event);
                        } else {
                            events.add(event);
                        }
                    }

                    // Qualsiasi tipo diverso da quelli attesi genera errore
                    default -> throw new IllegalArgumentException(
                            "Tipo non valido in " + resourcePath + ": " + type
                    );
                }
            }

            // Restituisce l'oggetto finale con tribeCharacters, events e finalEvents.
            // La lista buildings qui è ancora vuota, perché viene caricata separatamente.
            return new LoadedCards(tribeCharacters, events, finalEvents, new ArrayList<>());

        } catch (Exception e) {
            // Wrappa qualsiasi errore in una RuntimeException con messaggio più chiaro
            throw new RuntimeException("Errore nel caricamento di tribeCharacter/event", e);
        }
    }

    /**
     * Controlla se un nodo JSON rappresenta un evento finale.
     *
     * @param node nodo JSON della carta
     * @return true se l'id è 95 o 96, false altrimenti
     */
    private boolean isFinalEventNode(JsonNode node) {
        int id = node.get("id").asInt();
        return id == FINAL_EVENT_ID_1 || id == FINAL_EVENT_ID_2;
    }

    /**
     * Converte un nodo JSON in un oggetto TribeCharacter concreto.
     *
     * In base al campo characterType crea la sottoclasse corretta:
     * Hunter, Builder, Shaman, Inventor, Artist o Collector.
     *
     * @param node nodo JSON della carta tribeCharacter
     * @return oggetto TribeCharacter
     */
    private TribeCharacter parseTribeCharacter(JsonNode node) {
        try {
            // Lettura dei campi comuni a tutte le carte personaggio
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            CharacterType characterType = CharacterType.valueOf(node.get("characterType").asText());

            // Nodo annidato contenente i dati specifici dell'effetto
            JsonNode effectNode = node.get("effect");

            // Creazione della sottoclasse concreta in base al tipo di personaggio
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

                case INVENTOR -> new Inventor(
                        id,
                        era,
                        minPlayers,
                        (char) effectNode.get("iconPerInventor").asInt()
                );

                case ARTIST -> new Artist(id, era, minPlayers);
                case COLLECTOR -> new Collector(id, era, minPlayers);
            };

        } catch (Exception e) {
            throw new RuntimeException("Errore nel parsing di tribeCharacter", e);
        }
    }

    /**
     * Converte un nodo JSON in un oggetto Event concreto.
     *
     * In base al campo eventType crea la sottoclasse corretta.
     *
     * @param node nodo JSON della carta event
     * @return oggetto Event
     */
    private Event parseEvent(JsonNode node) {
        try {
            // Lettura dei campi comuni a tutti gli eventi
            String id = node.get("id").asText();
            Era era = Era.valueOf(node.get("era").asText());
            int minPlayers = node.get("minPlayers").asInt();
            EventType eventType = EventType.valueOf(node.get("eventType").asText());

            // Creazione della sottoclasse concreta dell'evento
            return switch (eventType) {
                case HUNTING -> new Hunting(
                        id,
                        era,
                        minPlayers,
                        EventType.HUNTING,
                        null
                );

                case SUSTENANCE -> new sustenance(
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
            throw new RuntimeException("Errore nel parsing di event", e);
        }
    }

    /**
     * Carica tutti i building da un file JSON.
     *
     * @param resourcePath percorso del file JSON dei building
     * @return lista di building caricati
     */
    public List<Building> loadBuildings(String resourcePath) {
        List<Building> buildings = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            // Verifica che il file esista
            if (is == null) {
                throw new IllegalArgumentException("File non trovato: " + resourcePath);
            }

            // Legge il JSON
            JsonNode root = mapper.readTree(is);

            // Anche qui la radice deve essere un array
            if (!root.isArray()) {
                throw new IllegalArgumentException("La radice del JSON dei building deve essere un array");
            }

            // Converte ogni nodo JSON in un Building
            for (JsonNode node : root) {
                buildings.add(parseBuilding(node));
            }

            return buildings;

        } catch (Exception e) {
            throw new RuntimeException("Errore nel caricamento dei building", e);
        }
    }

    /**
     * Converte un nodo JSON in un oggetto Building.
     *
     * @param node nodo JSON del building
     * @return oggetto Building
     */
    private Building parseBuilding(JsonNode node) {
        // Lettura dei campi principali del building
        String id = node.get("id").asText();
        Era era = Era.valueOf(node.get("era").asText());
        int minPlayers = 2; // da tua scelta progettuale, i building hanno sempre minPlayers = 2
        int foodPrice = node.get("foodPrice").asInt();
        int finalPP = node.get("finalPP").asInt();

        // Tipo di effetto del building
        String effectType = node.get("BuildingEffectType").asText();

        // Nodo annidato con i dati specifici dell'effetto
        JsonNode effectNode = node.get("effect");

        // Parsing dell'effetto concreto
        BuildingEffect effect = parseBuildingEffect(effectType, effectNode, id);

        // Creazione dell'oggetto finale Building
        return new Building(id, era, minPlayers, foodPrice, finalPP, effect);
    }

    /**
     * Converte i dati dell'effetto del building nell'oggetto concreto corretto.
     *
     * @param effectType tipo di effetto letto dal JSON
     * @param effectNode nodo JSON contenente i dati dell'effetto
     * @param buildingId id del building, utile per messaggi di errore
     * @return oggetto BuildingEffect concreto
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
                    "BuildingEffectType sconosciuto nel building " + buildingId + ": " + effectType
            );
        };
    }

    /**
     * Legge un CharacterType che può anche essere nullo.
     *
     * Questo metodo serve nei casi in cui il campo targetCharacterType
     * nel JSON possa essere assente oppure valorizzato a null.
     *
     * @param node nodo JSON da leggere
     * @return CharacterType corrispondente, oppure null
     */
    private CharacterType readNullableCharacterType(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return CharacterType.valueOf(node.asText());
    }

    /**
     * Carica tutte le carte del gioco:
     * - tribeCharacters
     * - events
     * - finalEvents
     * - buildings
     *
     * @param tribeEventPath percorso del JSON con tribeCharacter ed event
     * @param buildingPath percorso del JSON con i building
     * @return LoadedCards completo
     */
    public LoadedCards loadAllCards(String tribeEventPath, String buildingPath) {
        // Prima carica tribeCharacter, event e finalEvent
        LoadedCards partial = loadTribeCharactersAndEvents(tribeEventPath);

        // Poi carica i building
        List<Building> buildings = loadBuildings(buildingPath);

        // Costruisce l'oggetto finale con tutte le carte
        return new LoadedCards(
                partial.getTribeCharacters(),
                partial.getEvents(),
                partial.getFinalEvents(),
                buildings
        );
    }
}