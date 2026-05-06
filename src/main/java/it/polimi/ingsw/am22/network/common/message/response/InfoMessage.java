package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;

/**
 * Non-critical informational message from server to clients.
 *
 * Used e.g. to notify voluntary disconnects or events
 * that do not require a specific action but the user should know about.
 *
 * @param message informational text
 */
public record InfoMessage(String message) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
