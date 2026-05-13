package it.polimi.ingsw.am22.network.client;

import java.util.List;

/**
 * Commands the client can send to the server.
 *
 * <p>Decouples the client controller from the concrete transport (socket / RMI).
 * Most game requests carry a {@code matchId} — provided by the server in response
 * to {@link #createMatch(String, int)} or {@link #addPlayerToLobby(String, String)}.
 */
public interface ServerConnection {

    void listMatches();

    void createMatch(String hostNickname, int expectedPlayers);

    void addPlayerToLobby(String matchId, String nickname);

    void setExpectedPlayers(String matchId, String requesterNickname, int expectedPlayers);

    void removePlayerFromLobby(String matchId, String nickname);

    void placeTotem(String matchId, String playerNickname, char offerLetter);

    void pickCards(String matchId, String playerNickname, List<String> selectedCardIds);

    void pickBonusCard(String matchId, String playerNickname, String bonusCardId);

    void disconnectPlayer(String matchId, String nickname);
}
