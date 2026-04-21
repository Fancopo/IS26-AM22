package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Astrazione del canale di comunicazione verso un singolo client.
 *
 * Permette a {@link NetworkGameService} e {@link VirtualView} di inviare
 * messaggi e gestire disconnessioni senza conoscere il trasporto sottostante
 * (socket oppure callback RMI). Le due implementazioni concrete sono
 * {@link SocketClientHandler} e {@link RmiClientChannel}.
 */
public interface ClientChannel {

    /**
     * Invia un messaggio al client associato a questo canale.
     *
     * @param message messaggio da inviare
     */
    void send(ServerMessage message);

    /**
     * Chiude il canale liberando le risorse sottostanti.
     */
    void close();

    /**
     * Restituisce il nickname a cui questo canale è associato.
     *
     * @return nickname, oppure {@code null} se non ancora associato
     */
    String getBoundNickname();

    /**
     * Associa (o disassocia, con {@code null}) un nickname a questo canale.
     *
     * @param nickname nickname da associare, oppure {@code null} per rimuovere l'associazione
     */
    void setBoundNickname(String nickname);
}
