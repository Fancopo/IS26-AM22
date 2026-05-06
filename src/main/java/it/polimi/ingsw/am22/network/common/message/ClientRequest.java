package it.polimi.ingsw.am22.network.common.message;

/**
 * Base interface for all requests the client sends to the server.
 *
 * Each implementation accepts a {@link ClientRequestVisitor} to enable
 * polymorphic dispatch without instanceof.
 */
public interface ClientRequest extends NetworkMessage {
    void accept(ClientRequestVisitor visitor);
}
