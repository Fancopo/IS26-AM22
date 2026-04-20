package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

public record EndGameMessage(WinnerDTO winner, GameStateDTO finalGameState) implements ServerMessage {
}
