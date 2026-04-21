package it.polimi.ingsw.am22.network.common.message;

/**
 * Marker interface per tutti i messaggi che il server invia al client.
 *
 * Comprende sia notifiche di stato (lobby, gioco, fine partita)
 * sia messaggi informativi o di errore.
 */
public interface ServerMessage extends NetworkMessage {
}
