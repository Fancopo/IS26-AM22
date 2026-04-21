package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Callback che la view del client implementa per ricevere gli aggiornamenti dal server.
 *
 * Viene invocato in modo asincrono dal reader thread (socket) o dal thread RMI:
 * l'implementazione deve essere thread-safe oppure delegare l'aggiornamento UI
 * al thread corretto (es. EDT di Swing o thread JavaFX).
 */
public interface ClientUpdateHandler {

    /**
     * Invocato ogni volta che arriva un messaggio dal server.
     *
     * @param message messaggio ricevuto
     */
    void onServerMessage(ServerMessage message);

    /**
     * Invocato quando la connessione viene chiusa, sia volontariamente sia per errore.
     *
     * @param cause causa della chiusura ({@code null} se chiusura pulita)
     */
    default void onConnectionClosed(Throwable cause) {
    }
}
