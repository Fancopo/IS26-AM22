package it.polimi.ingsw.am22.network.client.connection;

import it.polimi.ingsw.am22.network.client.ServerHandler;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.CreateMatchRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.DisconnectPlayerRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ListMatchesRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ReconnectRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.SetExpectedPlayersRequest;

import java.util.List;

/**
 * Commands the client can send to the server, plus the lifecycle hooks
 * (handler registration and close) used by the transport.
 *
 * <p>Decouples the client from the concrete transport (socket / RMI):
 * implementations only need to provide {@link #send(ClientRequest)},
 * {@link #setMessageDispatcher} and {@link #close()}. All command methods
 * are {@code default} and build the appropriate {@link ClientRequest}
 * in one place, so adding a new command requires editing only this file
 * plus the corresponding request record.
 *
 * <p>Most game requests carry a {@code matchId} — provided by the server
 * in response to {@link #createMatch(String, int)} or
 * {@link #addPlayerToLobby(String, String)}.
 */
public interface ServerConnection extends AutoCloseable {

    /** Registers the handler invoked on every incoming server message. */
    void setMessageDispatcher(ServerHandler handler);

    @Override
    void close();

    /** Transport-specific delivery of a request to the server. */
    void send(ClientRequest request);

    default void listMatches() {
        send(new ListMatchesRequest());
    }

    default void createMatch(String hostNickname, int expectedPlayers) {
        send(new CreateMatchRequest(hostNickname, expectedPlayers));
    }

    default void addPlayerToLobby(String matchId, String nickname) {
        send(new AddPlayerToLobbyRequest(matchId, nickname));
    }

    default void setExpectedPlayers(String matchId, String requesterNickname, int expectedPlayers) {
        send(new SetExpectedPlayersRequest(matchId, requesterNickname, expectedPlayers));
    }

    default void removePlayerFromLobby(String matchId, String nickname) {
        send(new RemovePlayerFromLobbyRequest(matchId, nickname));
    }

    default void placeTotem(String matchId, String playerNickname, char offerLetter) {
        send(new PlaceTotemRequest(matchId, playerNickname, offerLetter));
    }

    default void pickCards(String matchId, String playerNickname, List<String> selectedCardIds) {
        send(new PickCardsRequest(matchId, playerNickname, selectedCardIds));
    }

    default void pickBonusCard(String matchId, String playerNickname, String bonusCardId) {
        send(new PickBonusCardRequest(matchId, playerNickname, bonusCardId));
    }

    default void disconnectPlayer(String matchId, String nickname) {
        send(new DisconnectPlayerRequest(matchId, nickname));
    }

    default void reconnect(String matchId, String nickname) {
        send(new ReconnectRequest(matchId, nickname));
    }
}
