package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Application-level liveness probe sent by the client to the server.
 * Filtered by the transport layer (the read loop drops it before dispatch),
 * so it never reaches the controller — its only purpose is to keep the
 * write path exercised so a brutally dropped TCP connection surfaces
 * quickly instead of waiting for the OS TCP keepalive (~2 h).
 */
public record PingRequest() implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
