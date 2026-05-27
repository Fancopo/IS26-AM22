package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

/**
 * Application-level liveness probe sent by the server to the client.
 * Filtered by the transport layer (the read loop drops it before dispatch),
 * so it never reaches the view. See {@link it.polimi.ingsw.am22.network.protocol.message.request.PingRequest}
 * for the rationale.
 */
public record PingMessage() implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
