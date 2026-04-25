package it.polimi.ingsw.am22.network.client;

import java.util.List;

/**
 * Interfaccia che rappresenta i comandi che il client può inviare al server.
 *
 * Lo scopo di questa interfaccia è separare il controller client
 * dal tipo concreto di connessione usata sotto (per esempio socket o RMI).
 *
 * Con il supporto a partite multiple, quasi tutte le richieste di gioco
 * richiedono un {@code matchId} che identifica la partita a cui la richiesta
 * è diretta. Il matchId viene fornito dal server in risposta a
 * {@link #createMatch(String, int)} o {@link #addPlayerToLobby(String, String)}.
 */
public interface ServerConnection {

    /**
     * Richiede al server la lista delle partite aperte e non ancora iniziate.
     */
    void listMatches();

    /**
     * Richiede al server la creazione di una nuova partita.
     *
     * @param hostNickname nickname del creatore (diventerà host)
     * @param expectedPlayers numero di giocatori attesi (2-5)
     */
    void createMatch(String hostNickname, int expectedPlayers);

    /**
     * Richiede al server di aggiungere un giocatore alla lobby di una partita esistente.
     *
     * @param matchId identificativo della partita a cui unirsi
     * @param nickname nickname del giocatore che entra nella lobby
     */
    void addPlayerToLobby(String matchId, String nickname);

    /**
     * Richiede al server di impostare il numero totale di giocatori attesi in una partita.
     *
     * @param matchId identificativo della partita
     * @param requesterNickname nickname di chi fa la richiesta
     * @param expectedPlayers numero totale di giocatori desiderato
     */
    void setExpectedPlayers(String matchId, String requesterNickname, int expectedPlayers);

    /**
     * Richiede al server di rimuovere un giocatore dalla lobby di una partita.
     *
     * @param matchId identificativo della partita
     * @param nickname nickname del giocatore da rimuovere
     */
    void removePlayerFromLobby(String matchId, String nickname);

    /**
     * Invia al server il comando di piazzamento del totem.
     *
     * @param matchId identificativo della partita
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param offerLetter lettera della tessera scelta
     */
    void placeTotem(String matchId, String playerNickname, char offerLetter);

    /**
     * Invia al server il comando di scelta delle carte.
     *
     * @param matchId identificativo della partita
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param selectedCardIds lista degli id delle carte selezionate
     */
    void pickCards(String matchId, String playerNickname, List<String> selectedCardIds);

    /**
     * Invia al server il comando di scelta della carta bonus.
     *
     * @param matchId identificativo della partita
     * @param playerNickname nickname del giocatore che compie l'azione
     * @param bonusCardId id della carta bonus scelta
     */
    void pickBonusCard(String matchId, String playerNickname, String bonusCardId);

    /**
     * Notifica al server la disconnessione di un giocatore.
     *
     * @param matchId identificativo della partita
     * @param nickname nickname del giocatore disconnesso
     */
    void disconnectPlayer(String matchId, String nickname);
}
