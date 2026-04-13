package it.polimi.ingsw.am22.controller;

import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class GameController {
    private static final List<String> DEFAULT_TOTEM_COLORS = List.of("red", "blue", "green", "yellow", "black");

    private final List<Consumer<GameSnapshot>> listeners;
    private Game game;

    public GameController() {
        this.listeners = new ArrayList<>();
    }

    public GameSnapshot createGameWithDefaultTotems(List<String> nicknames) {
        Objects.requireNonNull(nicknames, "nicknames cannot be null");

        if (nicknames.size() > DEFAULT_TOTEM_COLORS.size()) {
            throw new IllegalArgumentException("Too many players for the available default totem colors.");
        }

        List<PlayerRegistration> players = new ArrayList<>();
        for (int i = 0; i < nicknames.size(); i++) {
            players.add(new PlayerRegistration(nicknames.get(i), DEFAULT_TOTEM_COLORS.get(i)));
        }

        return createGame(players);
    }

    public GameSnapshot createGame(List<PlayerRegistration> registrations) {
        Objects.requireNonNull(registrations, "registrations cannot be null");

        if (registrations.size() < 2 || registrations.size() > 5) {
            throw new IllegalArgumentException("The game supports from 2 to 5 players.");
        }

        ensureUniqueNicknames(registrations);
        ensureUniqueColors(registrations);

        List<Player> players = new ArrayList<>();
        for (PlayerRegistration registration : registrations) {
            Player player = new Player(registration.nickname());
            player.setTotem(new Totem(registration.totemColor(), player));
            players.add(player);
        }

        this.game = new Game(players);
        return notifyAndSnapshot();
    }

    public boolean hasGame() {
        return game != null;
    }

    public GameSnapshot startMatch() {
        Game currentGame = requireGame();
        currentGame.startMatch();
        return notifyAndSnapshot();
    }

    public GameSnapshot placeTotem(String playerNickname, char offerLetter) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        OfferTile tile = findOfferTile(Character.toUpperCase(offerLetter));

        currentGame.placeTotemOnOffer(player, tile);
        return notifyAndSnapshot();
    }

    public GameSnapshot pickCards(String playerNickname, Collection<String> selectedCardIds) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        List<Card> selectedCards = resolveBoardCards(selectedCardIds);

        currentGame.pickCards(player, selectedCards);
        return notifyAndSnapshot();
    }

    public GameSnapshot pickBonusCard(String playerNickname, String bonusCardId) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        Card bonusCard = findUpperRowCard(requireText(bonusCardId, "bonusCardId"));

        currentGame.pickBonusCard(player, bonusCard);
        return notifyAndSnapshot();
    }

    public GameSnapshot resolveEvents() {
        Game currentGame = requireGame();
        currentGame.resolveEvents();
        return notifyAndSnapshot();
    }

    public GameSnapshot updateRound() {
        Game currentGame = requireGame();
        currentGame.updateRound();
        return notifyAndSnapshot();
    }

    public WinnerSnapshot determineWinner() {
        Game currentGame = requireGame();
        Player winner = currentGame.determineWinner();
        GameSnapshot snapshot = notifyAndSnapshot();
        return new WinnerSnapshot(
                winner.getNickname(),
                Optional.ofNullable(winner.getTotem()).map(Totem::getColor).orElse(null),
                winner.finalPP(),
                winner.getFood(),
                snapshot
        );
    }

    public GameSnapshot getSnapshot() {
        return buildSnapshot(requireGame());
    }

    public void addListener(Consumer<GameSnapshot> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener cannot be null"));
    }

    public void removeListener(Consumer<GameSnapshot> listener) {
        listeners.remove(listener);
    }

    private void ensureUniqueNicknames(List<PlayerRegistration> registrations) {
        Set<String> uniqueNicknames = new LinkedHashSet<>();
        for (PlayerRegistration registration : registrations) {
            String normalized = normalize(registration.nickname());
            if (!uniqueNicknames.add(normalized)) {
                throw new IllegalArgumentException("Player nicknames must be unique.");
            }
        }
    }

    private void ensureUniqueColors(List<PlayerRegistration> registrations) {
        Set<String> uniqueColors = new LinkedHashSet<>();
        for (PlayerRegistration registration : registrations) {
            String normalized = normalize(registration.totemColor());
            if (!uniqueColors.add(normalized)) {
                throw new IllegalArgumentException("Totem colors must be unique.");
            }
        }
    }

    private Game requireGame() {
        if (game == null) {
            throw new IllegalStateException("No game has been created yet.");
        }
        return game;
    }

    private Player requireActivePlayer(String playerNickname) {
        Player player = findPlayer(playerNickname);
        Player activePlayer = requireGame().getActivePlayer();

        if (activePlayer != null && activePlayer != player) {
            throw new IllegalStateException("It is not " + playerNickname + "'s turn.");
        }

        return player;
    }

    private Player findPlayer(String playerNickname) {
        String normalizedNickname = normalize(requireText(playerNickname, "playerNickname"));

        return requireGame().getPlayers().stream()
                .filter(player -> normalize(player.getNickname()).equals(normalizedNickname))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + playerNickname));
    }

    private OfferTile findOfferTile(char offerLetter) {
        return requireGame().getBoard().getOfferTrack().stream()
                .filter(tile -> Character.toUpperCase(tile.getLetter()) == offerLetter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown offer tile: " + offerLetter));
    }

    private Card findUpperRowCard(String cardId) {
        Card card = requireGame().getBoard().getUpperRow().stream()
                .filter(c -> c.getId().equals(cardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown upper-row card: " + cardId));

        if (card instanceof Event) {
            throw new IllegalArgumentException("Events cannot be selected as bonus cards.");
        }

        return card;
    }

    private List<Card> resolveBoardCards(Collection<String> selectedCardIds) {
        if (selectedCardIds == null || selectedCardIds.isEmpty()) {
            return List.of();
        }

        List<Card> availableCards = new ArrayList<>();
        availableCards.addAll(requireGame().getBoard().getUpperRow());
        availableCards.addAll(requireGame().getBoard().getLowerRow());

        List<Card> selectedCards = new ArrayList<>();
        Set<String> uniqueIds = new LinkedHashSet<>();

        for (String rawCardId : selectedCardIds) {
            String cardId = requireText(rawCardId, "cardId");
            if (!uniqueIds.add(cardId)) {
                throw new IllegalArgumentException("Duplicate card id in selection: " + cardId);
            }

            Card card = availableCards.stream()
                    .filter(candidate -> candidate.getId().equals(cardId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Card not available on the board: " + cardId));

            if (card instanceof Event) {
                throw new IllegalArgumentException("Events cannot be selected as tribe cards: " + cardId);
            }

            selectedCards.add(card);
        }

        return selectedCards;
    }

    private GameSnapshot notifyAndSnapshot() {
        GameSnapshot snapshot = buildSnapshot(requireGame());
        for (Consumer<GameSnapshot> listener : List.copyOf(listeners)) {
            listener.accept(snapshot);
        }
        return snapshot;
    }

    private GameSnapshot buildSnapshot(Game currentGame) {
        Board board = currentGame.getBoard();

        return new GameSnapshot(
                currentGame.getCurrentRound(),
                currentGame.getCurrentEra(),
                currentGame.getCurrentPhaseName(),
                Optional.ofNullable(currentGame.getActivePlayer()).map(Player::getNickname).orElse(null),
                currentGame.getPlayers().stream()
                        .map(player -> toPlayerSnapshot(player, currentGame.getActivePlayer()))
                        .toList(),
                new BoardSnapshot(
                        board.getUpperRow().stream().map(this::toCardSnapshot).toList(),
                        board.getLowerRow().stream().map(this::toCardSnapshot).toList(),
                        board.getOfferTrack().stream().map(this::toOfferTileSnapshot).toList(),
                        board.getTurnOrderTile().getSlots().stream().map(this::toTurnSlotSnapshot).toList()
                )
        );
    }

    private PlayerSnapshot toPlayerSnapshot(Player player, Player activePlayer) {
        Tribe tribe = player.getTribe();

        return new PlayerSnapshot(
                player.getNickname(),
                Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                player.getPP(),
                player.getFood(),
                player.finalPP(),
                player == activePlayer,
                tribe.getMembers().stream().map(this::toCardSnapshot).toList(),
                tribe.getBuildings().stream().map(this::toCardSnapshot).toList()
        );
    }

    private OfferTileSnapshot toOfferTileSnapshot(OfferTile tile) {
        return new OfferTileSnapshot(
                tile.getLetter(),
                tile.getUpperCardsToTake(),
                tile.getLowerCardsToTake(),
                tile.getFoodReward(),
                Optional.ofNullable(tile.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    private TurnSlotSnapshot toTurnSlotSnapshot(Slot slot) {
        return new TurnSlotSnapshot(
                slot.getPositionIndex(),
                slot.getFoodBonus(),
                slot.isLastSpace(),
                Optional.ofNullable(slot.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    private CardSnapshot toCardSnapshot(Card card) {
        String category = categoryOf(card);
        String detailType = detailTypeOf(card);
        int foodCost = card instanceof Building building ? building.getFoodPrice() : card.getFoodCost();

        return new CardSnapshot(
                card.getId(),
                category,
                detailType,
                card.getEra(),
                card.getMinPlayers(),
                foodCost
        );
    }

    private String categoryOf(Card card) {
        if (card instanceof TribeCharacter) {
            return "CHARACTER";
        }
        if (card instanceof Building) {
            return "BUILDING";
        }
        if (card instanceof Event) {
            return "EVENT";
        }
        return card.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    private String detailTypeOf(Card card) {
        if (card instanceof TribeCharacter character) {
            return character.getCharacterType().name();
        }
        if (card instanceof Building) {
            return "BUILDING";
        }
        if (card instanceof Event event) {
            return event.getEventType().name();
        }
        return card.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }

    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }

    public record PlayerRegistration(String nickname, String totemColor) {
        public PlayerRegistration {
            if (nickname == null || nickname.isBlank()) {
                throw new IllegalArgumentException("nickname cannot be null or blank.");
            }
            if (totemColor == null || totemColor.isBlank()) {
                throw new IllegalArgumentException("totemColor cannot be null or blank.");
            }
        }
    }

    public record GameSnapshot(
            int currentRound,
            Era currentEra,
            String currentPhase,
            String activePlayer,
            List<PlayerSnapshot> players,
            BoardSnapshot board
    ) {
        public GameSnapshot {
            players = List.copyOf(players);
            Objects.requireNonNull(board, "board cannot be null");
        }
    }

    public record PlayerSnapshot(
            String nickname,
            String totemColor,
            int prestigePoints,
            int food,
            int projectedFinalPrestigePoints,
            boolean active,
            List<CardSnapshot> tribeCharacters,
            List<CardSnapshot> buildings
    ) {
        public PlayerSnapshot {
            tribeCharacters = List.copyOf(tribeCharacters);
            buildings = List.copyOf(buildings);
        }
    }

    public record BoardSnapshot(
            List<CardSnapshot> upperRow,
            List<CardSnapshot> lowerRow,
            List<OfferTileSnapshot> offerTrack,
            List<TurnSlotSnapshot> turnOrder
    ) {
        public BoardSnapshot {
            upperRow = List.copyOf(upperRow);
            lowerRow = List.copyOf(lowerRow);
            offerTrack = List.copyOf(offerTrack);
            turnOrder = turnOrder.stream()
                    .sorted(Comparator.comparingInt(TurnSlotSnapshot::positionIndex))
                    .toList();
        }
    }

    public record CardSnapshot(
            String id,
            String category,
            String detailType,
            Era era,
            int minPlayers,
            int foodCost
    ) { }

    public record OfferTileSnapshot(
            char letter,
            int upperCardsToTake,
            int lowerCardsToTake,
            int foodReward,
            String occupiedBy
    ) { }

    public record TurnSlotSnapshot(
            int positionIndex,
            int foodBonus,
            boolean lastSpace,
            String occupiedBy
    ) { }

    public record WinnerSnapshot(
            String nickname,
            String totemColor,
            int finalPrestigePoints,
            int remainingFood,
            GameSnapshot snapshot
    ) { }
}
