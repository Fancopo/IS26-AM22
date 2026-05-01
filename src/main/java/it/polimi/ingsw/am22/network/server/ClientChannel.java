package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.server.rmi.RmiClientChannel;
import it.polimi.ingsw.am22.network.server.socket.SocketClientHandler;

/**
 * interfaccia astratta del singolo destinatario (send/close/bound nickname).
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
