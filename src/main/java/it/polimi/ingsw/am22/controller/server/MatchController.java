package it.polimi.ingsw.am22.controller.server;

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
public class MatchController {

    /**
     * Palette dei colori di totem selezionabili dai giocatori nella fase di
     * scelta che precede l'inizio della partita.
     */
    private static final List<String> TOTEM_PALETTE =
            List.of("Red", "Blue", "White", "Yellow", "Black");

    /** Identificativo univoco della partita gestita da questo controller. */
    private final String matchId;

    /** Lista dei giocatori attualmente presenti nella lobby. */
    private final List<Player> lobbyPlayers;

    /** Riferimento alla partita vera e propria; rimane null finché il match non parte. */
    private Game game;

    /** Nickname del giocatore host, cioè il primo entrato in lobby. */
    private String hostNickname;

    /** Numero di giocatori scelto dall'host per avviare la partita. */
    private int expectedPlayers;

    /**
     * True quando la lobby è piena e i giocatori stanno scegliendo il totem,
     * a turno, prima che la partita inizi davvero.
     */
    private boolean selectingTotem;

    /**
     * Indice (nell'ordine di ingresso, {@link #lobbyPlayers}) del giocatore a
     * cui tocca scegliere il totem durante la fase di selezione.
     */
    private int totemPickIndex;

    /**
     * Costruisce un controller inizialmente vuoto.
     * La lobby è aperta e nessuna partita è ancora iniziata.
     *
     * @param matchId identificativo del match gestito da questo controller
     */
    public MatchController(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId cannot be null or blank.");
        }
        this.matchId = matchId;
        this.lobbyPlayers = new ArrayList<>();
        this.game = null;
        this.hostNickname = null;
        this.expectedPlayers = 0;
        this.selectingTotem = false;
        this.totemPickIndex = 0;
    }

    /**
     * Rebuilds a controller around a game restored from disk after a server
     * crash. The match is already past the lobby phase, so {@code lobbyPlayers}
     * is re-populated from the game's players only to keep the two views
     * consistent for the (unused, post-start) lobby getters.
     *
     * @param matchId         id of the restored match
     * @param game            the in-progress game loaded from a snapshot
     * @param hostNickname    nickname of the original host
     * @param expectedPlayers number of players the match was started with
     */
    public MatchController(String matchId, Game game, String hostNickname, int expectedPlayers) {
        this(matchId);
        if (game == null) {
            throw new IllegalArgumentException("game cannot be null when restoring a match.");
        }
        this.game = game;
        this.hostNickname = hostNickname;
        this.expectedPlayers = expectedPlayers;
        this.lobbyPlayers.addAll(game.getPlayers());
    }

    /**
     * Restituisce l'identificativo del match gestito da questo controller.
     *
     * @return matchId univoco
     */
    public String getMatchId() {
        return matchId;
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
     * - il nickname non sia già presente (confronto case-insensitive,
     *   {@link Locale#ROOT}: "Alice" e "alice" sono considerati lo stesso
     *   nickname, ma la casing originale viene conservata per la visualizzazione)
     *
     * Inoltre assegna automaticamente un totem e, se necessario,
     * imposta il primo giocatore come host.
     *
     * @param nickname nickname del giocatore da aggiungere
     */
    public void addPlayerToLobby(String nickname) {
        requireLobbyOpen();
        requireNotSelectingTotem();
        String cleanNickname = requireText(nickname, "nickname");

        if (lobbyPlayers.size() >= 5) {
            throw new IllegalStateException("The lobby is already full.");
        }

        if (containsNickname(cleanNickname)) {
            throw new IllegalArgumentException("Nickname already in use: " + cleanNickname);
        }

        // Il totem NON viene assegnato all'ingresso: i giocatori lo scelgono
        // a turno nella fase di selezione che precede l'inizio della partita.
        Player player = new Player(cleanNickname);
        lobbyPlayers.add(player);

        if (hostNickname == null) {
            hostNickname = cleanNickname;
        }

        // Dopo ogni ingresso si verifica se si può avviare la selezione totem.
        tryBeginTotemSelection();
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
        requireNotSelectingTotem();
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
        tryBeginTotemSelection();
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

        // Se qualcuno esce durante la scelta dei totem, la fase di selezione si
        // annulla: i totem già scelti vengono liberati e i giocatori rimasti
        // tornano in lobby (la selezione ripartirà quando la lobby si riempie).
        if (selectingTotem) {
            resetTotemSelection();
        }
    }

    // --- Fase di selezione del totem ----------------------------------------

    /**
     * Indica se la lobby è nella fase di scelta dei totem (lobby piena, partita
     * non ancora avviata, giocatori che scelgono il colore a turno).
     *
     * @return true se la selezione dei totem è in corso
     */
    public boolean isSelectingTotem() {
        return selectingTotem;
    }

    /**
     * Restituisce la palette dei colori di totem selezionabili.
     *
     * @return lista (immutabile) dei colori disponibili nel gioco
     */
    public List<String> getTotemPalette() {
        return TOTEM_PALETTE;
    }

    /**
     * Restituisce il nickname del giocatore a cui tocca scegliere il totem,
     * oppure null se non si è in fase di selezione.
     *
     * @return nickname del chooser corrente, o null
     */
    public String getCurrentTotemChooser() {
        if (!selectingTotem || totemPickIndex >= lobbyPlayers.size()) {
            return null;
        }
        return lobbyPlayers.get(totemPickIndex).getNickname();
    }

    /**
     * Assegna il colore di totem scelto dal giocatore di turno e avanza la
     * selezione. Quando l'ultimo giocatore ha scelto, la partita parte.
     *
     * Controlla che:
     * - la fase di selezione sia in corso;
     * - sia effettivamente il turno del giocatore indicato (ordine d'ingresso);
     * - il colore appartenga alla palette;
     * - il colore non sia già stato scelto da un altro giocatore.
     *
     * @param nickname nickname del giocatore che sta scegliendo
     * @param color    colore di totem desiderato
     */
    public void chooseTotem(String nickname, String color) {
        if (!selectingTotem) {
            throw new IllegalStateException("Totem selection is not in progress.");
        }
        String cleanNickname = requireText(nickname, "nickname");
        String cleanColor = requireText(color, "color");

        Player chooser = lobbyPlayers.get(totemPickIndex);
        if (!normalize(chooser.getNickname()).equals(normalize(cleanNickname))) {
            throw new IllegalStateException("It is not this player's turn to choose a totem.");
        }

        String canonicalColor = TOTEM_PALETTE.stream()
                .filter(c -> c.equalsIgnoreCase(cleanColor.strip()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown totem color: " + color));

        if (isColorTaken(canonicalColor)) {
            throw new IllegalArgumentException("Totem color already chosen: " + canonicalColor);
        }

        chooser.setTotem(new Totem(canonicalColor, chooser));
        totemPickIndex++;

        if (totemPickIndex >= lobbyPlayers.size()) {
            startGameNow();
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
     * Avvia la fase di selezione dei totem quando la lobby si riempie.
     *
     * La selezione comincia solo se:
     * - la partita non è già iniziata
     * - non si è già in selezione
     * - l'host ha scelto il numero atteso di giocatori
     * - il numero di giocatori in lobby coincide con quello atteso
     *
     * Durante la selezione i giocatori scelgono il totem a turno; la partita
     * vera e propria parte da {@link #chooseTotem} quando tutti hanno scelto.
     */
    private void tryBeginTotemSelection() {
        if (game != null || selectingTotem) {
            return;
        }

        if (expectedPlayers == 0) {
            return;
        }

        if (lobbyPlayers.size() != expectedPlayers) {
            return;
        }

        selectingTotem = true;
        totemPickIndex = 0;
    }

    /**
     * Crea e avvia il {@link Game} a fine selezione: a questo punto ogni
     * giocatore ha già scelto il proprio totem.
     */
    private void startGameNow() {
        ensureUniqueNicknames();
        this.game = new Game(new ArrayList<>(lobbyPlayers));
        game.startMatch();
        selectingTotem = false;
    }

    /** Verifica che un colore della palette non sia già stato scelto. */
    private boolean isColorTaken(String canonicalColor) {
        return lobbyPlayers.stream()
                .map(Player::getTotem)
                .filter(t -> t != null)
                .anyMatch(t -> t.getColor().equalsIgnoreCase(canonicalColor));
    }

    /** Annulla la fase di selezione liberando i totem già scelti. */
    private void resetTotemSelection() {
        selectingTotem = false;
        totemPickIndex = 0;
        for (Player p : lobbyPlayers) {
            p.setTotem(null);
        }
    }

    /** Vieta operazioni di lobby mentre è in corso la scelta dei totem. */
    private void requireNotSelectingTotem() {
        if (selectingTotem) {
            throw new IllegalStateException("Totem selection is in progress.");
        }
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
