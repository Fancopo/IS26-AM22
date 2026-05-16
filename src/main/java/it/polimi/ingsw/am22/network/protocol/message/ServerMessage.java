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
     * Whether this is the last message the server will send on this channel
     * (e.g. end of game, after which the server closes the connection).
     * Transports use it to decide when to fire a synthetic disconnect after
     * delivery. Note: an abnormally closed match is NOT terminal — the server
     * keeps the channel open so the client can return to the matches list.
     */
    default boolean isTerminal() { return false; }
}
