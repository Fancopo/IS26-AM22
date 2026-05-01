package it.polimi.ingsw.am22.controller;

import it.polimi.ingsw.am22.model.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Controller server unico.
 * Gestisce lobby (addPlayerToLobby, setExpectedPlayers, removePlayerFromLobby),
 * avvia il Game quando la lobby è completa,
 * espone azioni di partita (placeTotem, pickCards, pickBonusCard, determineWinner).
 * Risolve id stringa → oggetti del model e valida il giocatore attivo.
 */
public class GameController {

    /**
     * Colori assegnati automaticamente ai totem in ordine di ingresso nella lobby.
     */
    private static final List<String> DEFAULT_TOTEM_COLORS =
            List.of("Red", "Blue", "White", "Yellow", "Black");

    /** Lista dei giocatori attualmente presenti nella lobby. */
    private final List<Player> lobbyPlayers;

    /** Riferimento alla partita vera e propria; rimane null finché il match non parte. */
    private Game game;

    /** Nickname del giocatore host, cioè il primo entrato in lobby. */
    private String hostNickname;

    /** Numero di giocatori scelto dall'host per avviare la partita. */
    private int expectedPlayers;

    /**
     * Costruisce un controller inizialmente vuoto.
     * La lobby è aperta e nessuna partita è ancora iniziata.
     */
    public GameController() {
        this.lobbyPlayers = new ArrayList<>();
        this.game = null;
        this.hostNickname = null;
        this.expectedPlayers = 0;
    }

    /**
     * Indica se la partita è già stata avviata.
     *
     * @return true se il model Game è stato creato, false altrimenti
     */
    public boolean hasStarted() {
        return game != null;
    }

    /**
     * Restituisce la partita corrente.
     *
     * @return l'istanza del model Game, oppure null se il match non è partito
     */
    public Game getGame() {
        return game;
    }

    /**
     * Restituisce il nickname dell'host attuale della lobby.
     *
     * @return nickname host, oppure null se la lobby è vuota
     */
    public String getHostNickname() {
        return hostNickname;
    }

    /**
     * Restituisce il numero di giocatori scelto per avviare la partita.
     *
     * @return numero atteso di partecipanti
     */
    public int getExpectedPlayers() {
        return expectedPlayers;
    }

    /**
     * Restituisce una copia dei giocatori attualmente presenti in lobby.
     *
     * @return nuova lista contenente i giocatori della lobby
     */
    public List<Player> getLobbyPlayers() {
        return new ArrayList<>(lobbyPlayers);
    }

    /**
     * Aggiunge un nuovo giocatore alla lobby.
     *
     * Il metodo controlla che:
     * - la lobby sia ancora aperta
     * - non si superi il massimo di 5 giocatori
     * - il nickname non sia già presente
     *
     * Inoltre assegna automaticamente un totem e, se necessario,
     * imposta il primo giocatore come host.
     *
     * @param nickname nickname del giocatore da aggiungere
     */
    public void addPlayerToLobby(String nickname) {
        requireLobbyOpen();
        String cleanNickname = requireText(nickname, "nickname");

        if (lobbyPlayers.size() >= 5) {
            throw new IllegalStateException("The lobby is already full.");
        }

        if (containsNickname(cleanNickname)) {
            throw new IllegalArgumentException("Nickname already in use: " + cleanNickname);
        }

        Player player = new Player(cleanNickname);
        player.setTotem(new Totem(DEFAULT_TOTEM_COLORS.get(lobbyPlayers.size()), player));
        lobbyPlayers.add(player);

        if (hostNickname == null) {
            hostNickname = cleanNickname;
        }

        // Dopo ogni ingresso si verifica se la partita può partire.
        tryStartGame();
    }

    /**
     * Permette all'host di scegliere il numero totale di giocatori attesi.
     *
     * Il numero deve essere compreso tra 2 e 5 e non può essere inferiore
     * al numero di giocatori già connessi in lobby.
     *
     * @param requesterNickname nickname di chi sta facendo la richiesta
     * @param expectedPlayers numero di giocatori desiderato per la partita
     */
    public void setExpectedPlayers(String requesterNickname, int expectedPlayers) {
        requireLobbyOpen();
        String cleanNickname = requireText(requesterNickname, "requesterNickname");

        if (!cleanNickname.equals(hostNickname)) {
            throw new IllegalStateException("Only the host can choose the number of players.");
        }

        if (expectedPlayers < 2 || expectedPlayers > 5) {
            throw new IllegalArgumentException("The number of players must be between 2 and 5.");
        }

        if (lobbyPlayers.size() > expectedPlayers) {
            throw new IllegalStateException("There are already more connected players than the selected limit.");
        }

        this.expectedPlayers = expectedPlayers;
        tryStartGame();
    }

    /**
     * Rimuove un giocatore dalla lobby.
     *
     * Se esce l'host, il nuovo host diventa il primo giocatore rimasto.
     * Se il numero atteso non è più coerente con i giocatori presenti,
     * viene azzerato e dovrà essere scelto nuovamente.
     *
     * @param nickname nickname del giocatore da rimuovere
     */
    public void removePlayerFromLobby(String nickname) {
        requireLobbyOpen();
        Player player = findLobbyPlayer(nickname);
        lobbyPlayers.remove(player);

        if (player.getNickname().equals(hostNickname)) {
            hostNickname = lobbyPlayers.isEmpty() ? null : lobbyPlayers.getFirst().getNickname();
        }

        if (expectedPlayers > 0 && lobbyPlayers.size() > expectedPlayers) {
            expectedPlayers = 0;
        }
    }

    /**
     * Esegue l'azione di piazzamento del totem su una tessera offerta.
     *
     * @param playerNickname nickname del giocatore attivo
     * @param offerLetter lettera della tessera scelta
     */
    public void placeTotem(String playerNickname, char offerLetter) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        OfferTile tile = findOfferTile(Character.toUpperCase(offerLetter));

        if (!tile.isAvailable()) {
            throw new IllegalArgumentException("The selected offer tile is already occupied.");
        }

        currentGame.placeTotemOnOffer(player, tile);
    }

    /**
     * Esegue l'azione di scelta delle carte da parte del giocatore attivo.
     *
     * @param playerNickname nickname del giocatore attivo
     * @param selectedCardIds lista degli id delle carte selezionate
     */
    public void pickCards(String playerNickname, List<String> selectedCardIds) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        List<Card> selectedCards = resolveCardsFromBoard(selectedCardIds);

        currentGame.pickCards(player, selectedCards);
    }

    /**
     * Esegue la scelta della carta bonus di fine round.
     *
     * @param playerNickname nickname del giocatore attivo
     * @param bonusCardId id della carta bonus selezionata
     */
    public void pickBonusCard(String playerNickname, String bonusCardId) {
        Game currentGame = requireGame();
        Player player = requireActivePlayer(playerNickname);
        Card bonusCard = findUpperRowCard(requireText(bonusCardId, "bonusCardId"));

        currentGame.pickBonusCard(player, bonusCard);
    }

    /**
     * Chiede al model di determinare il vincitore finale.
     *
     * @return giocatore vincitore
     */
    public Player determineWinner() {
        return requireGame().determineWinner();
    }

    /**
     * Prova ad avviare la partita se tutte le condizioni sono soddisfatte.
     *
     * La partita parte solo se:
     * - non è già iniziata
     * - l'host ha scelto il numero atteso di giocatori
     * - il numero di giocatori in lobby coincide con quello atteso
     */
    private void tryStartGame() {
        if (game != null) {
            return;
        }

        if (expectedPlayers == 0) {
            return;
        }

        if (lobbyPlayers.size() != expectedPlayers) {
            return;
        }

        ensureUniqueNicknames();
        this.game = new Game(new ArrayList<>(lobbyPlayers));
        game.startMatch();
    }

    /**
     * Controlla che tutti i nickname in lobby siano univoci.
     */
    private void ensureUniqueNicknames() {
        Set<String> nicknames = new LinkedHashSet<>();
        for (Player player : lobbyPlayers) {
            String normalized = normalize(player.getNickname());
            if (!nicknames.add(normalized)) {
                throw new IllegalArgumentException("Player nicknames must be unique.");
            }
        }
    }

    /**
     * Verifica che la lobby sia ancora aperta.
     * Se la partita è già iniziata, non sono più consentite operazioni di lobby.
     */
    private void requireLobbyOpen() {
        if (game != null) {
            throw new IllegalStateException("The match has already started.");
        }
    }

    /**
     * Restituisce la partita corrente, sollevando eccezione se non è ancora iniziata.
     *
     * @return partita corrente
     */
    private Game requireGame() {
        if (game == null) {
            throw new IllegalStateException("The match has not started yet.");
        }
        return game;
    }

    /**
     * Verifica che il giocatore indicato esista nella partita e sia il giocatore attivo.
     *
     * @param nickname nickname del giocatore da controllare
     * @return il Player corrispondente se il controllo va a buon fine
     */
    private Player requireActivePlayer(String nickname) {
        Player player = findMatchPlayer(nickname);
        Player activePlayer = requireGame().getActivePlayer();

        if (activePlayer != null && activePlayer != player) {
            throw new IllegalStateException("It is not this player's turn.");
        }

        return player;
    }

    /**
     * Cerca un giocatore nella lobby a partire dal nickname.
     *
     * @param nickname nickname del giocatore cercato
     * @return giocatore trovato nella lobby
     */
    private Player findLobbyPlayer(String nickname) {
        String normalized = normalize(requireText(nickname, "nickname"));

        return lobbyPlayers.stream()
                .filter(player -> normalize(player.getNickname()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown lobby player: " + nickname));
    }

    /**
     * Cerca un giocatore nella partita a partire dal nickname.
     *
     * @param nickname nickname del giocatore cercato
     * @return giocatore trovato nella partita
     */
    private Player findMatchPlayer(String nickname) {
        String normalized = normalize(requireText(nickname, "nickname"));

        return requireGame().getPlayers().stream()
                .filter(player -> normalize(player.getNickname()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown player: " + nickname));
    }

    /**
     * Controlla se un nickname è già presente nella lobby.
     *
     * @param nickname nickname da verificare
     * @return true se il nickname esiste già, false altrimenti
     */
    private boolean containsNickname(String nickname) {
        String normalized = normalize(nickname);
        return lobbyPlayers.stream()
                .anyMatch(player -> normalize(player.getNickname()).equals(normalized));
    }

    /**
     * Cerca una tessera offerta tramite la sua lettera identificativa.
     *
     * @param offerLetter lettera della tessera
     * @return tessera trovata sul tracciato delle offerte
     */
    private OfferTile findOfferTile(char offerLetter) {
        return requireGame().getBoard().getOfferTrack().stream()
                .filter(tile -> Character.toUpperCase(tile.getLetter()) == offerLetter)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown offer tile: " + offerLetter));
    }

    /**
     * Converte una lista di id nelle corrispondenti carte presenti sul tabellone.
     *
     * Il metodo controlla che:
     * - la lista non contenga duplicati
     * - ogni carta selezionata sia effettivamente presente sul board
     *
     * @param selectedCardIds lista di id ricevuti dal client
     * @return lista delle carte reali del model
     */
    private List<Card> resolveCardsFromBoard(List<String> selectedCardIds) {
        if (selectedCardIds == null || selectedCardIds.isEmpty()) {
            return List.of();
        }

        List<Card> availableCards = new ArrayList<>();
        availableCards.addAll(requireGame().getBoard().getUpperRow());
        availableCards.addAll(requireGame().getBoard().getLowerRow());

        List<Card> selectedCards = new ArrayList<>();
        Set<String> uniqueIds = new LinkedHashSet<>();

        for (String rawId : selectedCardIds) {
            String cardId = requireText(rawId, "cardId");

            if (!uniqueIds.add(cardId)) {
                throw new IllegalArgumentException("Duplicate card id in selection: " + cardId);
            }

            Card card = availableCards.stream()
                    .filter(candidate -> candidate.getId().equals(cardId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Card not available on the board: " + cardId));

            selectedCards.add(card);
        }

        return selectedCards;
    }

    /**
     * Cerca una carta bonus nella riga superiore del tabellone.
     *
     * @param bonusCardId id della carta bonus
     * @return carta trovata
     */
    private Card findUpperRowCard(String bonusCardId) {
        return requireGame().getBoard().getUpperRow().stream()
                .filter(card -> card.getId().equals(bonusCardId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown upper-row card: " + bonusCardId));
    }

    /**
     * Controlla che una stringa non sia null o vuota.
     *
     * @param value valore da controllare
     * @param fieldName nome logico del campo, usato nei messaggi di errore
     * @return la stringa stessa se valida
     */
    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }

    /**
     * Normalizza una stringa per confronti case-insensitive e senza spazi esterni.
     *
     * @param value stringa da normalizzare
     * @return stringa normalizzata
     */
    private String normalize(String value) {
        return value.strip().toLowerCase(Locale.ROOT);
    }
}
