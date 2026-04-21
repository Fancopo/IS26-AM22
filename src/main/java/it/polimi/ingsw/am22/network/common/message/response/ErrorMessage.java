package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Messaggio di errore inviato dal server al singolo client che ha causato il problema.
 *
 * Usato ad esempio per richieste non valide, eccezioni del controller
 * o payload non riconosciuti.
 *
 * @param message descrizione dell'errore
 */
public record ErrorMessage(String message) implements ServerMessage {
}
