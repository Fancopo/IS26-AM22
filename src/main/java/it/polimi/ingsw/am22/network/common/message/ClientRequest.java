package it.polimi.ingsw.am22.network.common.message;

/**
 * Interfaccia base per tutte le richieste che il client invia al server.
 *
 * Ogni implementazione accetta un {@link ClientRequestVisitor} per consentire
 * il dispatch polimorfo senza instanceof.
 */
public interface ClientRequest extends NetworkMessage {
    void accept(ClientRequestVisitor visitor);
}
