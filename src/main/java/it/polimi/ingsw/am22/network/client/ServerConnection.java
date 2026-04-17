package it.polimi.ingsw.am22.network.client;

import java.util.List;

/**
 * Interfaccia che rappresenta i comandi che il client può inviare al server.
 *
 * Lo scopo di questa interfaccia è separare il controller client
 * dal tipo concreto di connessione usata sotto (per esempio socket o RMI).
 */
public interface ServerConnection {

    /**
     * Richiede al server di aggiungere un giocatore alla lobby.
     *
     * @param nickname nickname del giocatore che entra nella lobby
     */
    void addPlayerToLobby(String nickname);

    /**
     * Richiede al server di impostare il numero totale di giocatori attesi.
     *
     * @param requesterNickname nickname di chi fa la richiesta
     * @param expectedPlayers numero totale di giocatori desiderato
     */
    void setExpectedPlayers(String requesterNickname, int expectedPlayers);

    /**
     * Richiede al server di rimuovere un giocatore dalla lobby.
     *
     * @param nickname nickname del giocatore da rimuovere
     */
    void removePlayerFromLobby(String nickname);

    /**
     * Invia al server il comando di piazzamento del totem.
     *
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param offerLetter lettera della tessera scelta
     */
    void placeTotem(String playerNickname, char offerLetter);

    /**
     * Invia al server il comando di scelta delle carte.
     *
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param selectedCardIds lista degli id delle carte selezionate
     */
    void pickCards(String playerNickname, List<String> selectedCardIds);

    /**
     * Invia al server il comando di scelta della carta bonus.
     *
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param bonusCardId id della carta bonus scelta
     */
    void pickBonusCard(String playerNickname, String bonusCardId);

    /**
     * Notifica al server la disconnessione di un giocatore.
     *
     * @param nickname nickname del giocatore disconnesso
     */
    void disconnectPlayer(String nickname);
}
