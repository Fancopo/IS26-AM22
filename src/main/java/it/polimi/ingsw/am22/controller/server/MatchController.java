package it.polimi.ingsw.am22.controller.server;

import it.polimi.ingsw.am22.model.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Single server-side controller for one match. Manages the lobby
 * (addPlayerToLobby, setExpectedPlayers, removePlayerFromLobby), starts the
 * {@link Game} when the lobby is full, and exposes the in-match actions
 * (placeTotem, pickCards, pickBonusCard, determineWinner). It resolves string
 * ids to model objects and validates that the acting player is the active one.
 */
public class MatchController {

    /**
     * Palette of totem colours players can choose from during the selection
     * phase that precedes the start of the match.
     */
    private static final List<String> TOTEM_PALETTE =
            List.of("Red", "Blue", "White", "Yellow", "Black");

    /** Unique id of the match handled by this controller. */
    private final String matchId;

    /** Players currently in the lobby. */
    private final List<Player> lobbyPlayers;

    /** The actual game; stays null until the match starts. */
    private Game game;

    /** Nickname of the host player, i.e. the first to join the lobby. */
    private String hostNickname;

    /** Number of players the host chose in order to start the match. */
    private int expectedPlayers;

    /**
     * True when the lobby is full and players are choosing their totems, in
     * turn, before the match actually begins.
     */
    private boolean selectingTotem;

    /**
     * Index (in join order, {@link #lobbyPlayers}) of the player whose turn it
     * is to choose a totem during the selection phase.
     */
    private int totemPickIndex;

    /**
     * Builds an initially empty controller: the lobby is open and no match has
     * started yet.
     *
     * @param matchId id of the match handled by this controller
     */
    public MatchController(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId cannot be null or blank.");
        }
        this.matchId = matchId;
        this.lobbyPlayers = new ArrayList<>();
        this.game = null;
        this.hostNickname = null;
        this.expectedPlayers = 0;
        this.selectingTotem = false;
        this.totemPickIndex = 0;
    }

    /**
     * Rebuilds a controller around a game restored from disk after a server
     * crash. The match is already past the lobby phase, so {@code lobbyPlayers}
     * is re-populated from the game's players only to keep the two views
     * consistent for the (unused, post-start) lobby getters.
     *
     * @param matchId         id of the restored match
     * @param game            the in-progress game loaded from a snapshot
     * @param hostNickname    nickname of the original host
     * @param expectedPlayers number of players the match was started with
     */
    public MatchController(String matchId, Game game, String hostNickname, int expectedPlayers) {
        this(matchId);
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null when restoring a match.");
        }
        this.game = game;
        this.hostNickname = hostNickname;
        this.expectedPlayers = expectedPlayers;
        this.lobbyPlayers.addAll(game.getPlayers());
    }

    /**
     * @return the unique id of the match handled by this controller
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * @return whether the match has already started (the {@link Game} model exists)
     */
    public boolean hasStarted() {
        return game != null;
    }

    /**
     * @return the current game, or null if the match has not started
     */
    public Game getGame() {
        return game;
    }

    /**
     * @return the current lobby host's nickname, or null if the lobby is empty
     */
    public String getHostNickname() {
        return hostNickname;
    }

    /**
     * @return the number of players chosen to start the match
     */
    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    /**
     * @return a copy of the players currently in the lobby
     */
    public List<Player> getLobbyPlayers() {
        return new ArrayList<>(lobbyPlayers);
    }

    /**
     * Adds a new player to the lobby.
     *
     * <p>Checks that the lobby is still open, that the 5-player maximum is not
     * exceeded, and that the nickname is not already taken (case-insensitive,
     * {@link Locale#ROOT}: "Alice" and "alice" are the same nickname, but the
     * original casing is kept for display). The first player to join becomes the
     * host. The totem is not assigned here: players choose it in turn during the
     * selection phase.
     *
     * @param nickname nickname of the player to add
     */
    public void addPlayerToLobby(String nickname) {
        requireLobbyOpen();
        requireNotSelectingTotem();
        String cleanNickname = requireText(nickname, "nickname");

        if (lobbyPlayers.size() >= 5) {
            throw new IllegalStateException("The lobby is already full.");
        }

        if (containsNickname(cleanNickname)) {
            throw new IllegalArgumentException("Nickname already in use: " + cleanNickname);
        }

        // The totem is NOT assigned on entry: players choose it in turn during
        // the selection phase that precedes the start of the match.
        Player player = new Player(cleanNickname);
        lobbyPlayers.add(player);

        if (hostNickname == null) {
            hostNickname = cleanNickname;
        }

        // After each join, check whether totem selection can begin.
        tryBeginTotemSelection();
    }

    /**
     * Lets the host choose the total number of expected players.
     *
     * <p>The number must be between 2 and 5 and not less than the number of
     * players already connected to the lobby.
     *
     * @param requesterNickname nickname of the requester
     * @param expectedPlayers   desired number of players for the match
     */
    public void setExpectedPlayers(String requesterNickname, int expectedPlayers) {
        requireLobbyOpen();
        requireNotSelectingTotem();
        String cleanNickname = requireText(requesterNickname, "requesterNickname");

        if (!cleanNickname.equals(hostNickname)) {
            throw new IllegalStateException("Only the host can choose the number of players.");
        }

        if (expectedPlayers < 2 || expectedPlayers > 5) {
            throw new IllegalArgumentException("The number of players must be between 2 and 5.");
        }

        if (lobbyPlayers.size() > expectedPlayers) {
            throw new IllegalStateException("There are already more connected players than the selected limit.");
        }

        this.expectedPlayers = expectedPlayers;
        tryBeginTotemSelection();
    }

    /**
     * Removes a player from the lobby.
     *
     * <p>If the host leaves, the new host is the first remaining player. If the
     * expected number is no longer consistent with the players present, it is
     * reset and must be chosen again.
     *
     * @param nickname nickname of the player to remove
     */
    public void removePlayerFromLobby(String nickname) {
        requireLobbyOpen();
        Player player = findLobbyPlayer(nickname);
        lobbyPlayers.remove(player);

        if (player.getNickname().equals(hostNickname)) {
            hostNickname = lobbyPlayers.isEmpty() ? null : lobbyPlayers.getFirst().getNickname();
        }

        if (expectedPlayers > 0 && lobbyPlayers.size() > expectedPlayers) {
            expectedPlayers = 0;
        }

        // If someone leaves during totem selection, the phase is cancelled: the
        // totems already chosen are freed and the remaining players go back to
        // the lobby (selection restarts when the lobby fills again).
        if (selectingTotem) {
            resetTotemSelection();
        }
    }

    // --- Totem selection phase ----------------------------------------------

    /**
     * @return true if the lobby is in the totem-selection phase (lobby full,
     *         match not started yet, players choosing their colour in turn)
     */
    public boolean isSelectingTotem() {
        return selectingTotem;
    }

    /**
     * @return the immutable list of selectable totem colours
     */
    public List<String> getTotemPalette() {
        return TOTEM_PALETTE;
    }

    /**
     * @return the nickname of the player whose turn it is to choose a totem, or
     *         null if not in the selection phase
     */
    public String getCurrentTotemChooser() {
        if (!selectingTotem || totemPickIndex >= lobbyPlayers.size()) {
            return null;
        }
        return lobbyPlayers.get(totemPickIndex).getNickname();
    }

    /**
     * Assigns the totem colour chosen by the current player and advances the
     * selection. When the last player has chosen, the match starts.
     *
     * <p>Checks that the selection phase is in progress, that it is really the
     * indicated player's turn (join order), that the colour is in the palette,
     * and that the colour has not already been taken by another player.
     *
     * @param nickname nickname of the player choosing
     * @param color    desired totem colour
     */
    public void chooseTotem(String nickname, String color) {
        if (!selectingTotem) {
            throw new IllegalStateException("Totem selection is not in progress.");
        }
        String cleanNickname = requireText(nickname, "nickname");
        String cleanColor = requireText(color, "color");

        Player chooser = lobbyPlayers.get(totemPickIndex);
        if (!normalize(chooser.getNickname()).equals(normalize(cleanNickname))) {
            throw new IllegalStateException("It is not this player's turn to choose a totem.");
        }

        String canonicalColor = TOTEM_PALETTE.stream()
                .filter(c -> c.equalsIgnoreCase(cleanColor.strip()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown totem color: " + color));

        if (isColorTaken(canonicalColor)) {
            throw new IllegalArgumentException("Totem color already chosen: " + canonicalColor);
        }

        chooser.setTotem(new Totem(canonicalColor, chooser));
        totemPickIndex++;

        if (totemPickIndex >= lobbyPlayers.size()) {
            startGameNow();
        }
    }

    /**
     * Performs the totem-placement action on an offer tile.
     *
     * @param playerNickname nickname of the active player
     * @param offerLetter    letter of the chosen tile
     */
    public void placeTotem(String playerNickname, char offerLetter) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        OfferTile tile = findOfferTile(Character.toUpperCase(offerLetter));

        if (!tile.isAvailable()) {
            throw new IllegalArgumentException("The selected offer tile is already occupied.");
        }

        currentGame.placeTotemOnOffer(player, tile);
    }

    /**
     * Performs the card-selection action for the active player.
     *
     * @param playerNickname  nickname of the active player
     * @param selectedCardIds ids of the selected cards
     */
    public void pickCards(String playerNickname, List<String> selectedCardIds) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        List<Card> selectedCards = resolveCardsFromBoard(selectedCardIds);

        currentGame.pickCards(player, selectedCards);
    }

    /**
     * Performs the end-of-round bonus card selection.
     *
     * @param playerNickname nickname of the active player
     * @param bonusCardId    id of the selected bonus card
     */
    public void pickBonusCard(String playerNickname, String bonusCardId) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        Card bonusCard = findUpperRowCard(requireText(bonusCardId, "bonusCardId"));

        currentGame.pickBonusCard(player, bonusCard);
    }

    /**
     * Asks the model to determine the final winner.
     *
     * @return the winning player
     */
    public Player determineWinner() {
        return requireGame().determineWinner();
    }

    /**
     * Begins the totem-selection phase once the lobby fills up.
     *
     * <p>Selection starts only if the match has not started, selection is not
     * already in progress, the host has chosen the expected number of players,
     * and the lobby size matches that number. During selection players choose
     * their totem in turn; the actual match starts from {@link #chooseTotem}
     * once everyone has chosen.
     */
    private void tryBeginTotemSelection() {
        if (game != null || selectingTotem) {
            return;
        }

        if (expectedPlayers == 0) {
            return;
        }

        if (lobbyPlayers.size() != expectedPlayers) {
            return;
        }

        selectingTotem = true;
        totemPickIndex = 0;
    }

    /**
     * Creates and starts the {@link Game} at the end of selection: by this point
     * every player has already chosen their totem.
     */
    private void startGameNow() {
        ensureUniqueNicknames();
        this.game = new Game(new ArrayList<>(lobbyPlayers));
        game.startMatch();
        selectingTotem = false;
    }

    /** Checks whether a palette colour has already been chosen. */
    private boolean isColorTaken(String canonicalColor) {
        return lobbyPlayers.stream()
                .map(Player::getTotem)
                .filter(t -> t != null)
                .anyMatch(t -> t.getColor().equalsIgnoreCase(canonicalColor));
    }

    /** Cancels the selection phase, freeing the totems already chosen. */
    private void resetTotemSelection() {
        selectingTotem = false;
        totemPickIndex = 0;
        for (Player p : lobbyPlayers) {
            p.setTotem(null);
        }
    }

    /** Forbids lobby operations while totem selection is in progress. */
    private void requireNotSelectingTotem() {
        if (selectingTotem) {
            throw new IllegalStateException("Totem selection is in progress.");
        }
    }

    /** Checks that all lobby nicknames are unique. */
    private void ensureUniqueNicknames() {
        Set<String> nicknames = new LinkedHashSet<>();
        for (Player player : lobbyPlayers) {
            String normalized = normalize(player.getNickname());
            if (!nicknames.add(normalized)) {
                throw new IllegalArgumentException("Player nicknames must be unique.");
            }
        }
    }

    /**
     * Checks that the lobby is still open. Once the match has started, lobby
     * operations are no longer allowed.
     */
    private void requireLobbyOpen() {
        if (game != null) {
            throw new IllegalStateException("The match has already started.");
        }
    }

    /**
     * Returns the current game, throwing if it has not started yet.
     *
     * @return the current game
     */
    private Game requireGame() {
        if (game == null) {
            throw new IllegalStateException("The match has not started yet.");
        }
        return game;
    }

    /**
     * Checks that the given player exists in the match and is the active player.
     *
     * @param nickname nickname of the player to check
     * @return the matching player if the check passes
     */
    private Player requireActivePlayer(String nickname) {
        Player player = findMatchPlayer(nickname);
        Player activePlayer = requireGame().getActivePlayer();

        if (activePlayer != null && activePlayer != player) {
            throw new IllegalStateException("It is not this player's turn.");
        }

        return player;
    }

    /**
     * Looks up a lobby player by nickname.
     *
     * @param nickname nickname of the player to find
     * @return the matching lobby player
     */
    private Player findLobbyPlayer(String nickname) {
        String normalized = normalize(requireText(nickname, "nickname"));

        return lobbyPlayers.stream()
                .filter(player -> normalize(player.getNickname()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown lobby player: " + nickname));
    }

    /**
     * Looks up a player of the match by nickname.
     *
     * @param nickname nickname of the player to find
     * @return the matching match player
     */
    private Player findMatchPlayer(String nickname) {
        String normalized = normalize(requireText(nickname, "nickname"));

        return requireGame().getPlayers().stream()
                .filter(player -> normalize(player.getNickname()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + nickname));
    }

    /**
     * Checks whether a nickname is already present in the lobby.
     *
     * @param nickname nickname to check
     * @return true if the nickname already exists, false otherwise
     */
    private boolean containsNickname(String nickname) {
        String normalized = normalize(nickname);
        return lobbyPlayers.stream()
                .anyMatch(player -> normalize(player.getNickname()).equals(normalized));
    }

    /**
     * Looks up an offer tile by its identifying letter.
     *
     * @param offerLetter the tile's letter
     * @return the matching tile on the offer track
     */
    private OfferTile findOfferTile(char offerLetter) {
        return requireGame().getBoard().getOfferTrack().stream()
                .filter(tile -> Character.toUpperCase(tile.getLetter()) == offerLetter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown offer tile: " + offerLetter));
    }

    /**
     * Converts a list of ids into the corresponding cards on the board.
     *
     * <p>Checks that the list contains no duplicates and that every selected
     * card is actually present on the board.
     *
     * @param selectedCardIds ids received from the client
     * @return the matching model cards
     */
    private List<Card> resolveCardsFromBoard(List<String> selectedCardIds) {
        if (selectedCardIds == null || selectedCardIds.isEmpty()) {
            return List.of();
        }

        List<Card> availableCards = new ArrayList<>();
        availableCards.addAll(requireGame().getBoard().getUpperRow());
        availableCards.addAll(requireGame().getBoard().getLowerRow());

        List<Card> selectedCards = new ArrayList<>();
        Set<String> uniqueIds = new LinkedHashSet<>();

        for (String rawId : selectedCardIds) {
            String cardId = requireText(rawId, "cardId");

            if (!uniqueIds.add(cardId)) {
                throw new IllegalArgumentException("Duplicate card id in selection: " + cardId);
            }

            Card card = availableCards.stream()
                    .filter(candidate -> candidate.getId().equals(cardId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Card not available on the board: " + cardId));

            selectedCards.add(card);
        }

        return selectedCards;
    }

    /**
     * Looks up a bonus card in the upper row of the board.
     *
     * @param bonusCardId id of the bonus card
     * @return the matching card
     */
    private Card findUpperRowCard(String bonusCardId) {
        return requireGame().getBoard().getUpperRow().stream()
                .filter(card -> card.getId().equals(bonusCardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown upper-row card: " + bonusCardId));
    }

    /**
     * Checks that a string is neither null nor blank.
     *
     * @param value     the value to check
     * @param fieldName logical field name, used in error messages
     * @return the string itself if valid
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }

    /**
     * Normalizes a string for case-insensitive comparison, trimming outer spaces.
     *
     * @param value the string to normalize
     * @return the normalized string
     */
    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
