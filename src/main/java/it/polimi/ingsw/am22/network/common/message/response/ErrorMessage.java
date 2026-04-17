package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

public record ErrorMessage(String message) implements ServerMessage {
}
