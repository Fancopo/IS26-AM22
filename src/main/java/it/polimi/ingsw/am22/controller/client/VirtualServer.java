package it.polimi.ingsw.am22.controller.client;

import it.polimi.ingsw.am22.network.client.connection.ServerConnection;

import java.util.List;
import java.util.Objects;

/**
 * Bridge between the client view and the server connection.
 *
 * <p>The view calls methods on this controller; the controller forwards
 * requests to the server, automatically attaching the local nickname and
 * matchId. The matchId is set after the server confirms create/join via
 * {@link #bindMatch(String, String)}, called from the update handler when
 * it sees a {@code MatchJoinedMessage}.
 */
public class VirtualServer {

    private final ServerConnection serverConnection;
    private String nickname;
    private String matchId;

    public VirtualServer(ServerConnection serverConnection) {
        this.serverConnection = Objects.requireNonNull(serverConnection, "serverConnection cannot be null");
    }

    public String getNickname() { return nickname; }
    public String getMatchId() { return matchId; }

    public boolean hasJoinedLobby() {
        return nickname != null && !nickname.isBlank()
                && matchId != null && !matchId.isBlank();
    }

    public void listMatches() {
        serverConnection.listMatches();
    }

    public void createMatch(String hostNickname, int expectedPlayers) {
        String cleanNickname = requireText(hostNickname, "hostNickname");
        this.nickname = cleanNickname;
        serverConnection.createMatch(cleanNickname, expectedPlayers);
    }

    public void addPlayerToLobby(String matchId, String nickname) {
        String cleanMatchId = requireText(matchId, "matchId");
        String cleanNickname = requireText(nickname, "nickname");
        this.nickname = cleanNickname;
        serverConnection.addPlayerToLobby(cleanMatchId, cleanNickname);
    }

    /** Called by the update handler when a MatchJoinedMessage arrives. */
    public void bindMatch(String matchId, String nickname) {
        this.matchId = requireText(matchId, "matchId");
        this.nickname = requireText(nickname, "nickname");
    }

    public void setExpectedPlayers(int expectedPlayers) {
        requireJoined();
        serverConnection.setExpectedPlayers(matchId, nickname, expectedPlayers);
    }

    public void removePlayerFromLobby() {
        requireJoined();
        serverConnection.removePlayerFromLobby(matchId, nickname);
        this.nickname = null;
        this.matchId = null;
    }

    public void placeTotem(char offerLetter) {
        requireJoined();
        serverConnection.placeTotem(matchId, nickname, offerLetter);
    }

    public void pickCards(List<String> selectedCardIds) {
        requireJoined();
        serverConnection.pickCards(matchId, nickname, selectedCardIds == null ? List.of() : selectedCardIds);
    }

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

    public void disconnect() {
        requireJoined();
        serverConnection.disconnectPlayer(matchId, nickname);
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
