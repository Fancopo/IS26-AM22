package it.polimi.ingsw.am22.network.protocol.message;

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

/**
 * Visitor for client-to-server requests. Dispatch goes through
 * {@link ClientRequest#accept} — no instanceof, a new request type becomes a
 * compile-time obligation on every implementor.
 */
public interface ClientRequestVisitor {
    void visit(CreateMatchRequest request);
    void visit(ListMatchesRequest request);
    void visit(AddPlayerToLobbyRequest request);
    void visit(SetExpectedPlayersRequest request);
    void visit(RemovePlayerFromLobbyRequest request);
    void visit(PlaceTotemRequest request);
    void visit(PickCardsRequest request);
    void visit(PickBonusCardRequest request);
    void visit(DisconnectPlayerRequest request);
    void visit(ReconnectRequest request);
}
