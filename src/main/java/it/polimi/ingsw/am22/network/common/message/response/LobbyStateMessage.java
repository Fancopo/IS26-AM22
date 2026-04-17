package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.view.LobbyStateView;

public record LobbyStateMessage(LobbyStateView lobbyState) implements ServerMessage {
}
