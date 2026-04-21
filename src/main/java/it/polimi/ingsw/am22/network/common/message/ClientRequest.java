package it.polimi.ingsw.am22.network.common.message;

/**
 * Marker interface per tutte le richieste che il client invia al server.
 *
 * Ogni azione del giocatore (join lobby, piazzamento totem, scelta carte, ecc.)
 * è rappresentata da un record che implementa questa interfaccia.
 */
public interface ClientRequest extends NetworkMessage {
}
