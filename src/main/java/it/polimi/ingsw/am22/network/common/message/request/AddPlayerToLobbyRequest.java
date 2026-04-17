package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

public record AddPlayerToLobbyRequest(String nickname) implements ClientRequest {
}
