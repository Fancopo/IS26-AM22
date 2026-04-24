package it.polimi.ingsw.am22.network.common.message;

/**
 * Interfaccia base per tutti i messaggi che il server invia al client.
 *
 * Ogni implementazione accetta un {@link ServerMessageVisitor} per consentire
 * il dispatch polimorfo senza instanceof.
 */
public interface ServerMessage extends NetworkMessage {
    void accept(ServerMessageVisitor visitor);
}
