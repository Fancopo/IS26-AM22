package it.polimi.ingsw.am22.controller.client;

import it.polimi.ingsw.am22.network.client.connection.ServerConnection;

import java.util.List;
import java.util.Objects;

/**
 * Local proxy of the server on the client side (the "Virtual Server" of the
 * distributed-MVC schema).
 *
 * <p>The view calls methods on this proxy as if it were the server; the proxy
 * forwards requests over the network, automatically attaching the local
 * nickname and matchId. The matchId is set after the server confirms
 * create/join via {@link #bindMatch(String, String)}, called from the update
 * handler when it sees a {@code MatchJoinedMessage}.
 */
public class VirtualServer {

    private final ServerConnection serverConnection;
    private String nickname;
    private String matchId;

    /**
     * @param serverConnection the transport used to reach the server
     */
    public VirtualServer(ServerConnection serverConnection) {
        this.serverConnection = Objects.requireNonNull(serverConnection, "serverConnection cannot be null");
    }

    /** @return the local nickname, or null if not set yet */
    public String getNickname() { return nickname; }

    /** @return the bound matchId, or null if not in a match */
    public String getMatchId() { return matchId; }

    /** @return whether both nickname and matchId are bound (the player is in a lobby/match) */
    public boolean hasJoinedLobby() {
        return nickname != null && !nickname.isBlank()
                && matchId != null && !matchId.isBlank();
    }

    /** Requests the list of open matches from the server. */
    public void listMatches() {
        serverConnection.listMatches();
    }

    /**
     * Creates a new match, registering the local player as host.
     *
     * @param hostNickname    the host's nickname
     * @param expectedPlayers the number of players the match should have
     */
    public void createMatch(String hostNickname, int expectedPlayers) {
        String cleanNickname = requireText(hostNickname, "hostNickname");
        this.nickname = cleanNickname;
        serverConnection.createMatch(cleanNickname, expectedPlayers);
    }

    /**
     * Joins the lobby of an existing match.
     *
     * @param matchId  id of the match to join
     * @param nickname the local player's nickname
     */
    public void addPlayerToLobby(String matchId, String nickname) {
        String cleanMatchId = requireText(matchId, "matchId");
        String cleanNickname = requireText(nickname, "nickname");
        this.nickname = cleanNickname;
        serverConnection.addPlayerToLobby(cleanMatchId, cleanNickname);
    }

    /**
     * Called by the update handler when a MatchJoinedMessage arrives.
     *
     * @param matchId  the confirmed match id
     * @param nickname the confirmed nickname
     */
    public void bindMatch(String matchId, String nickname) {
        this.matchId = requireText(matchId, "matchId");
        this.nickname = requireText(nickname, "nickname");
    }

    /**
     * Asks the host's server to change the expected player count.
     *
     * @param expectedPlayers the new expected player count
     */
    public void setExpectedPlayers(int expectedPlayers) {
        requireJoined();
        serverConnection.setExpectedPlayers(matchId, nickname, expectedPlayers);
    }

    /** Leaves the current lobby and clears the local binding. */
    public void removePlayerFromLobby() {
        requireJoined();
        serverConnection.removePlayerFromLobby(matchId, nickname);
        this.nickname = null;
        this.matchId = null;
    }

    /**
     * Chooses a totem colour during the pre-game selection phase.
     *
     * @param color the desired totem colour
     */
    public void chooseTotem(String color) {
        requireJoined();
        serverConnection.chooseTotem(matchId, nickname, requireText(color, "color"));
    }

    /**
     * Places the local player's totem on an offer tile.
     *
     * @param offerLetter the chosen tile's letter
     */
    public void placeTotem(char offerLetter) {
        requireJoined();
        serverConnection.placeTotem(matchId, nickname, offerLetter);
    }

    /**
     * Picks cards for the current action.
     *
     * @param selectedCardIds ids of the chosen cards (null is treated as empty)
     */
    public void pickCards(List<String> selectedCardIds) {
        requireJoined();
        serverConnection.pickCards(matchId, nickname, selectedCardIds == null ? List.of() : selectedCardIds);
    }

    /**
     * Picks the end-of-round bonus card.
     *
     * @param bonusCardId id of the chosen bonus card
     */
    public void pickBonusCard(String bonusCardId) {
        requireJoined();
        serverConnection.pickBonusCard(matchId, nickname, requireText(bonusCardId, "bonusCardId"));
    }

    /**
     * Clears the local match/nickname binding WITHOUT notifying the server:
     * used when the server tells us the match is gone (MatchClosedMessage),
     * so the client can return to the "matches list" state and reuse the
     * same connection for further list/create/join.
     */
    public void clearMatchBinding() {
        this.matchId = null;
        this.nickname = null;
    }

    /**
     * Rejoins a match that survived a server crash. Unlike a normal join the
     * matchId is already known by the client (it was kept across the drop):
     * both nickname and matchId are bound locally right away so any move sent
     * before the server's confirmation still routes to the right match.
     *
     * @param matchId  id of the suspended match to resume
     * @param nickname the local player's nickname
     */
    public void reconnect(String matchId, String nickname) {
        String cleanMatchId = requireText(matchId, "matchId");
        String cleanNickname = requireText(nickname, "nickname");
        this.nickname = cleanNickname;
        this.matchId = cleanMatchId;
        serverConnection.reconnect(cleanMatchId, cleanNickname);
    }

    /** Notifies the server that the local player is leaving the running match. */
    public void disconnect() {
        requireJoined();
        serverConnection.disconnectPlayer(matchId, nickname);
        this.nickname = null;
        this.matchId = null;
    }

    /**
     * Tells the server to tear down a suspended match the player chose not to
     * resume. No local binding is required (the client never reconnected): the
     * matchId is the one kept across the crash and is passed in explicitly.
     *
     * @param matchId id of the suspended match to abandon
     */
    public void abandonRecoveredMatch(String matchId) {
        serverConnection.abandonRecoveredMatch(requireText(matchId, "matchId"));
        this.nickname = null;
        this.matchId = null;
    }

    private void requireJoined() {
        if (!hasJoinedLobby()) {
            throw new IllegalStateException("You must join a match before sending actions.");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }
}
