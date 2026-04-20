package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;

public record GameStartedMessage(GameStateDTO initialGameState) implements ServerMessage {
}
