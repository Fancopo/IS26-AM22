package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

/**
 * Error message sent by the server to the single client that caused the issue.
 *
 * Used for example on invalid requests, controller exceptions
 * or unrecognized payloads.
 *
 * @param message error description
 */
public record ErrorMessage(String message) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
