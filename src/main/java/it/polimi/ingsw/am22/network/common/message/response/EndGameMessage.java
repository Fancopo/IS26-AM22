package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.view.GameStateView;
import it.polimi.ingsw.am22.network.common.view.WinnerView;

public record EndGameMessage(WinnerView winner, GameStateView finalGameState) implements ServerMessage {
}
