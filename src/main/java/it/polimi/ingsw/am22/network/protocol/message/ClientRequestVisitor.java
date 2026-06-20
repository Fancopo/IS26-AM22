package it.polimi.ingsw.am22.network.protocol.message;

import it.polimi.ingsw.am22.network.protocol.message.request.AbandonRecoveredMatchRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ChooseTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.CreateMatchRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.DisconnectPlayerRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ListMatchesRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PingRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.ReconnectRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.protocol.message.request.SetExpectedPlayersRequest;

/**
 * Visitor for client-to-server requests. Dispatch goes through
 * {@link ClientRequest#accept} — no instanceof, a new request type becomes a
 * compile-time obligation on every implementor.
 */
public interface ClientRequestVisitor {
    /** @param request the create-match request */
    void visit(CreateMatchRequest request);

    /** @param request the list-matches request */
    void visit(ListMatchesRequest request);

    /** @param request the add-player-to-lobby request */
    void visit(AddPlayerToLobbyRequest request);

    /** @param request the set-expected-players request */
    void visit(SetExpectedPlayersRequest request);

    /** @param request the remove-player-from-lobby request */
    void visit(RemovePlayerFromLobbyRequest request);

    /** @param request the choose-totem request */
    void visit(ChooseTotemRequest request);

    /** @param request the place-totem request */
    void visit(PlaceTotemRequest request);

    /** @param request the pick-cards request */
    void visit(PickCardsRequest request);

    /** @param request the pick-bonus-card request */
    void visit(PickBonusCardRequest request);

    /** @param request the disconnect-player request */
    void visit(DisconnectPlayerRequest request);

    /** @param request the reconnect request */
    void visit(ReconnectRequest request);

    /** @param request the abandon-recovered-match request */
    void visit(AbandonRecoveredMatchRequest request);

    /**
     * Transport-only liveness probe. Default no-op: visitor implementations
     * (controllers) should not care about pings — the transport read loop
     * is expected to drop the message before dispatch. Defaulted to avoid
     * forcing every implementor to add an empty method for a message that
     * is never meant to reach them.
     *
     * @param request the ping request
     */
    default void visit(PingRequest request) {}
}
