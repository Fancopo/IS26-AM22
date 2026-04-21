package it.polimi.ingsw.am22.network.common.message;

import it.polimi.ingsw.am22.network.common.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.common.message.request.DisconnectPlayerRequest;
import it.polimi.ingsw.am22.network.common.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.common.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.common.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.common.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.common.message.request.SetExpectedPlayersRequest;

/**
 * Visitor for {@link ClientRequest} hierarchies.
 *
 * <p>Each concrete {@code ClientRequest} implementation dispatches itself
 * to the correct overload of {@code handle(...)} via its {@code accept}
 * method, enabling proper double-dispatch polymorphism on the request
 * dispatcher without any {@code instanceof} / {@code switch} on the
 * runtime type.
 *
 * <p>Adding a new request type forces the compiler to require a new
 * overload here, keeping the dispatch table exhaustive at compile time.
 */
public interface ClientRequestHandler {

    void handle(AddPlayerToLobbyRequest request);

    void handle(SetExpectedPlayersRequest request);

    void handle(RemovePlayerFromLobbyRequest request);

    void handle(PlaceTotemRequest request);

    void handle(PickCardsRequest request);

    void handle(PickBonusCardRequest request);

    void handle(DisconnectPlayerRequest request);
}
