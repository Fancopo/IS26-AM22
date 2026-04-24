package it.polimi.ingsw.am22.network.common.message;

import it.polimi.ingsw.am22.network.common.message.request.AddPlayerToLobbyRequest;
import it.polimi.ingsw.am22.network.common.message.request.DisconnectPlayerRequest;
import it.polimi.ingsw.am22.network.common.message.request.PickBonusCardRequest;
import it.polimi.ingsw.am22.network.common.message.request.PickCardsRequest;
import it.polimi.ingsw.am22.network.common.message.request.PlaceTotemRequest;
import it.polimi.ingsw.am22.network.common.message.request.RemovePlayerFromLobbyRequest;
import it.polimi.ingsw.am22.network.common.message.request.SetExpectedPlayersRequest;

/**
 * Visitor per le richieste inviate dal client al server.
 *
 * Ogni implementazione definisce il comportamento per ciascun tipo di
 * {@link ClientRequest}: la dispatch avviene tramite {@link ClientRequest#accept}
 * senza usare instanceof.
 */
public interface ClientRequestVisitor {
    void visit(AddPlayerToLobbyRequest request);
    void visit(SetExpectedPlayersRequest request);
    void visit(RemovePlayerFromLobbyRequest request);
    void visit(PlaceTotemRequest request);
    void visit(PickCardsRequest request);
    void visit(PickBonusCardRequest request);
    void visit(DisconnectPlayerRequest request);
}
