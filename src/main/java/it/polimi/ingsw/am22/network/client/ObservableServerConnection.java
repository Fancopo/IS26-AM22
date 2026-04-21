package it.polimi.ingsw.am22.network.client;

/**
 * Estensione di {@link ServerConnection} che consente al client di ricevere
 * messaggi dal server in modo asincrono.
 *
 * Implementa {@link AutoCloseable} per gestire correttamente la chiusura
 * delle risorse sottostanti (socket o callback RMI).
 */
public interface ObservableServerConnection extends ServerConnection, AutoCloseable {

    /**
     * Registra l'handler che riceverà i {@code ServerMessage} inviati dal server.
     *
     * @param handler handler da invocare ad ogni messaggio ricevuto
     */
    void setClientUpdateHandler(ClientUpdateHandler handler);

    /**
     * Chiude la connessione al server liberando le risorse sottostanti.
     */
    @Override
    void close();
}
