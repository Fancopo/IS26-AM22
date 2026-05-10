package it.polimi.ingsw.am22.network.client;

import java.util.List;
import java.util.Objects;

/**
 * Controller lato client.
 *
 * Questa classe fa da ponte tra la view del client e l'oggetto di connessione
 * verso il server. La view chiama i metodi di questo controller e il controller
 * inoltra le richieste al server aggiungendo automaticamente il nickname locale
 * e il matchId della partita a cui il client è iscritto.
 *
 * Il matchId viene impostato:
 * - automaticamente dopo che il server conferma la creazione ({@code createMatch})
 *   o l'ingresso ({@code joinMatch}) tramite {@link #bindMatch(String, String)},
 *   che va invocato dall'update handler appena arriva un {@code MatchJoinedMessage}.
 */
public class ClientController {

    /** Connessione astratta verso il server. */
    private final ServerConnection serverConnection;

    /** Nickname del giocatore locale, salvato dopo l'ingresso in un match. */
    private String nickname;

    /** Identificativo della partita a cui il client è attualmente iscritto. */
    private String matchId;

    /**
     * Costruisce il controller client.
     *
     * @param serverConnection implementazione concreta della connessione al server
     */
    public ClientController(ServerConnection serverConnection) {
        this.serverConnection = Objects.requireNonNull(serverConnection, "serverConnection cannot be null");
        this.nickname = null;
        this.matchId = null;
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
     * Restituisce l'identificativo della partita a cui il client è iscritto.
     *
     * @return matchId, oppure null se non si è ancora iscritto a nessuna partita
     */
    public String getMatchId() {
        return matchId;
    }

    /**
     * Indica se il client ha già effettuato il join in una partita.
     *
     * @return true se nickname e matchId locali sono stati impostati
     */
    public boolean hasJoinedLobby() {
        return nickname != null && !nickname.isBlank()
                && matchId != null && !matchId.isBlank();
    }

    /**
     * Chiede al server la lista delle partite aperte.
     *
     * La risposta arriva in modo asincrono tramite {@code MatchesListMessage}
     * e dovrà essere gestita dall'update handler del client.
     */
    public void listMatches() {
        serverConnection.listMatches();
    }

    /**
     * Invia al server la richiesta di creazione di una nuova partita.
     *
     * Il nickname viene memorizzato localmente; il matchId sarà noto solo dopo la
     * risposta del server (vedi {@link #bindMatch(String, String)}).
     *
     * @param hostNickname nickname del creatore
     * @param expectedPlayers numero di giocatori attesi (2-5)
     */
    public void createMatch(String hostNickname, int expectedPlayers) {
        String cleanNickname = requireText(hostNickname, "hostNickname");
        this.nickname = cleanNickname;
        serverConnection.createMatch(cleanNickname, expectedPlayers);
    }

    /**
     * Invia al server la richiesta di ingresso in una lobby esistente.
     *
     * @param matchId identificativo della partita a cui unirsi
     * @param nickname nickname scelto dal giocatore
     */
    public void addPlayerToLobby(String matchId, String nickname) {
        String cleanMatchId = requireText(matchId, "matchId");
        String cleanNickname = requireText(nickname, "nickname");
        this.nickname = cleanNickname;
        serverConnection.addPlayerToLobby(cleanMatchId, cleanNickname);
    }

    /**
     * Memorizza i riferimenti alla partita a cui il client si è appena iscritto.
     *
     * Va invocato dall'update handler quando arriva un {@code MatchJoinedMessage}.
     *
     * @param matchId identificativo della partita
     * @param nickname nickname confermato dal server
     */
    public void bindMatch(String matchId, String nickname) {
        this.matchId = requireText(matchId, "matchId");
        this.nickname = requireText(nickname, "nickname");
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
        serverConnection.setExpectedPlayers(matchId, nickname, expectedPlayers);
    }

    /**
     * Rimuove il giocatore locale dalla lobby e cancella lo stato locale.
     */
    public void removePlayerFromLobby() {
        requireJoined();
        serverConnection.removePlayerFromLobby(matchId, nickname);
        this.nickname = null;
        this.matchId = null;
    }

    /**
     * Invia al server la mossa di piazzamento del totem.
     *
     * @param offerLetter lettera della tessera scelta
     */
    public void placeTotem(char offerLetter) {
        requireJoined();
        serverConnection.placeTotem(matchId, nickname, offerLetter);
    }

    /**
     * Invia al server la lista delle carte selezionate dal giocatore locale.
     *
     * @param selectedCardIds lista degli id delle carte selezionate
     */
    public void pickCards(List<String> selectedCardIds) {
        requireJoined();
        serverConnection.pickCards(matchId, nickname, selectedCardIds == null ? List.of() : selectedCardIds);
    }

    /**
     * Invia al server la scelta della carta bonus.
     *
     * @param bonusCardId id della carta bonus scelta
     */
    public void pickBonusCard(String bonusCardId) {
        requireJoined();
        serverConnection.pickBonusCard(matchId, nickname, requireText(bonusCardId, "bonusCardId"));
    }

    /**
     * Cancella il binding locale match/nickname senza notificare il server:
     * va invocato quando è il server stesso a comunicarci che il match non
     * esiste più (es. {@code MatchClosedMessage} per abort di un altro
     * giocatore). Dopo, il client torna allo stato "non in partita" e può
     * riemettere list/create/join sulla stessa connessione.
     */
    public void clearMatchBinding() {
        this.matchId = null;
        this.nickname = null;
    }

    /**
     * Notifica al server la disconnessione del giocatore locale.
     */
    public void disconnect() {
        requireJoined();
        serverConnection.disconnectPlayer(matchId, nickname);
        this.nickname = null;
        this.matchId = null;
    }

    private void requireJoined() {
        if (!hasJoinedLobby()) {
            throw new IllegalStateException("You must join a match before sending actions.");
        }
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }
}
