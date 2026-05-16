package it.polimi.ingsw.am22.network.protocol.message;

/**
 * Base interface for all messages the server sends to the client.
 *
 * Each implementation accepts a {@link ServerMessageVisitor} to enable
 * polymorphic dispatch without instanceof.
 */
public interface ServerMessage extends NetworkMessage {
    void accept(ServerMessageVisitor visitor);

    /**
     * Whether this is the last message the server will send for the current
     * match (e.g. end of game or abnormal closure). Transports use it to
     * decide when to fire a synthetic disconnect after delivery.
     */
    default boolean isTerminal() { return false; }
}
