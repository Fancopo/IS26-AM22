package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Messaggio informativo non critico inviato dal server ai client.
 *
 * Usato ad esempio per notificare disconnessioni volontarie o eventi
 * che non richiedono un'azione specifica ma che l'utente deve conoscere.
 *
 * @param message testo informativo
 */
public record InfoMessage(String message) implements ServerMessage {
}
