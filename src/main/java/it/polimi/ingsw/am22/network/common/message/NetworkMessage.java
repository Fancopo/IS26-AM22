package it.polimi.ingsw.am22.network.common.message;

import java.io.Serializable;

/**
 * Marker interface per tutti i messaggi che viaggiano sulla rete tra client e server.
 *
 * Estende {@link Serializable} perché ogni messaggio deve poter essere trasmesso
 * sia via socket (tramite {@code ObjectOutputStream}) sia via RMI.
 */
public interface NetworkMessage extends Serializable {
}
