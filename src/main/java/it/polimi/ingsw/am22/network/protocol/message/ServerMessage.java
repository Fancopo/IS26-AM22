package it.polimi.ingsw.am22.network.protocol.message;

/**
 * Base interface for all messages the server sends to the client.
 *
 * Each implementation accepts a {@link ServerMessageVisitor} to enable
 * polymorphic dispatch without instanceof.
 */
public interface ServerMessage extends NetworkMessage {
    void accept(ServerMessageVisitor visitor);
}
