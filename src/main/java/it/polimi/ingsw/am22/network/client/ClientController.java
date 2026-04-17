package it.polimi.ingsw.am22.network.client;

import java.util.List;
import java.util.Objects;

/**
 * Controller lato client.
 *
 * Questa classe fa da ponte tra la view del client e l'oggetto di connessione
 * verso il server. La view chiama i metodi di questo controller e il controller
 * inoltra le richieste al server aggiungendo automaticamente il nickname locale
 * del giocatore quando necessario.
 */
public class ClientController {

    /** Connessione astratta verso il server. */
    private final ServerConnection serverConnection;

    /** Nickname del giocatore locale, salvato dopo il join in lobby. */
    private String nickname;

    /**
     * Costruisce il controller client.
     *
     * @param serverConnection implementazione concreta della connessione al server
     */
    public ClientController(ServerConnection serverConnection) {
        this.serverConnection = Objects.requireNonNull(serverConnection, "serverConnection cannot be null");
        this.nickname = null;
    }

    /**
     * Restituisce il nickname locale del client.
     *
     * @return nickname del giocatore, oppure null se non ha ancora fatto join
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Indica se il client ha già effettuato il join nella lobby.
     *
     * @return true se il nickname locale è stato impostato
     */
    public boolean hasJoinedLobby() {
        return nickname != null && !nickname.isBlank();
    }

    /**
     * Invia al server la richiesta di ingresso nella lobby.
     *
     * Il nickname viene anche memorizzato localmente per le richieste successive.
     *
     * @param nickname nickname scelto dal giocatore
     */
    public void addPlayerToLobby(String nickname) {
        String cleanNickname = requireText(nickname, "nickname");
        this.nickname = cleanNickname;
        serverConnection.addPlayerToLobby(cleanNickname);
    }

    /**
     * Chiede al server di impostare il numero di giocatori attesi.
     *
     * Questo metodo può essere usato solo dopo essere entrati in lobby.
     *
     * @param expectedPlayers numero di giocatori scelto
     */
    public void setExpectedPlayers(int expectedPlayers) {
        requireJoined();
        serverConnection.setExpectedPlayers(nickname, expectedPlayers);
    }

    /**
     * Rimuove il giocatore locale dalla lobby e cancella il nickname salvato.
     */
    public void removePlayerFromLobby() {
        requireJoined();
        serverConnection.removePlayerFromLobby(nickname);
        this.nickname = null;
    }

    /**
     * Invia al server la mossa di piazzamento del totem.
     *
     * @param offerLetter lettera della tessera scelta
     */
    public void placeTotem(char offerLetter) {
        requireJoined();
        serverConnection.placeTotem(nickname, offerLetter);
    }

    /**
     * Invia al server la lista delle carte selezionate dal giocatore locale.
     *
     * Se la lista ricevuta è null, viene trasformata in una lista vuota.
     *
     * @param selectedCardIds lista degli id delle carte selezionate
     */
    public void pickCards(List<String> selectedCardIds) {
        requireJoined();
        serverConnection.pickCards(nickname, selectedCardIds == null ? List.of() : selectedCardIds);
    }

    /**
     * Invia al server la scelta della carta bonus.
     *
     * @param bonusCardId id della carta bonus scelta
     */
    public void pickBonusCard(String bonusCardId) {
        requireJoined();
        serverConnection.pickBonusCard(nickname, requireText(bonusCardId, "bonusCardId"));
    }

    /**
     * Notifica al server la disconnessione del giocatore locale.
     *
     * Dopo la disconnessione il nickname locale viene cancellato.
     */
    public void disconnect() {
        requireJoined();
        serverConnection.disconnectPlayer(nickname);
        this.nickname = null;
    }

    /**
     * Verifica che il client abbia già effettuato il join in lobby.
     */
    private void requireJoined() {
        if (!hasJoinedLobby()) {
            throw new IllegalStateException("You must join the lobby before sending actions.");
        }
    }

    /**
     * Controlla che una stringa non sia null o vuota.
     *
     * @param value valore da verificare
     * @param fieldName nome logico del campo, usato nel messaggio di errore
     * @return la stringa stessa se valida
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }
}
