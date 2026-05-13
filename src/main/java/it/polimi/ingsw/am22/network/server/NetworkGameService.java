package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LeaderboardEntryDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.common.message.response.*;
import it.polimi.ingsw.am22.network.server.databases.MatchResultDao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cuore del layer di rete lato server, in versione multipartita.
 *
 * <p>Mantiene un registry di {@link MatchSession}: ogni matchId è una partita
 * indipendente con il suo {@link GameController} e la sua {@link VirtualView}.
 * Le richieste di gioco contengono un {@code matchId} e vengono instradate
 * alla MatchSession corrispondente; le richieste di lobby globale
 * ({@link CreateMatchRequest}, {@link ListMatchesRequest}) sono gestite
 * direttamente qui.
 *
 * <p>Il dispatch avviene via {@link ClientRequestVisitor}: niente instanceof,
 * aggiungere un nuovo tipo di richiesta diventa un obbligo a compile-time.
 * Il metodo è {@code synchronized} per serializzare le mutazioni del registry
 * e delle singole sessioni; le partite in corso sono comunque poche e leggere.
 */
public class NetworkGameService {

    private final ModelDtoMapper mapper;

    /** Registry delle partite attive, indicizzate per matchId. */
    private final Map<String, MatchSession> matchesById;

    /** Generatore monotono di matchId leggibili (es. {@code M-1}, {@code M-2}, ...). */
    private final AtomicLong matchIdSeq;

    /** DAO per persistere i risultati delle partite finite e leggere la classifica storica. */
    private final MatchResultDao matchResultDao;

    /**
     * Scheduler used to defer the close of client channels after the end of a match.
     * Closing immediately after broadcasting {@link EndGameMessage} can race with the
     * client's read loop: the underlying socket is torn down before the EndGameMessage
     * has been fully consumed, and the client surfaces an EOFException instead of the
     * winner screen. Deferring the close gives every client time to read and render
     * the message; if a client disconnects sooner on its own that is fine, the close
     * is idempotent.
     */
    private final ScheduledExecutorService endGameCloser = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "endgame-closer");
        t.setDaemon(true);
        return t;
    });

    /** Grace period between the EndGameMessage broadcast and the channel close. */
    private static final long END_GAME_CLOSE_DELAY_MS = 3000;

    /**
     * Costruisce il servizio centrale del server. Crea il mapper modello->DTO,
     * il registry delle partite vuoto e il DAO per la persistenza dei risultati.
     * Invocato una sola volta dal {@link NetworkServerLauncher} all'avvio.
     */
    public NetworkGameService() {
        this.mapper = new ModelDtoMapper();
        this.matchesById = new ConcurrentHashMap<>();
        this.matchIdSeq = new AtomicLong();
        this.matchResultDao = new MatchResultDao();
    }

    /**
     * Punto d'ingresso unico per ogni richiesta che arriva dai trasporti
     * (Socket o RMI). Dispatcha la richiesta sul metodo handler corretto
     * usando il {@link ClientRequestVisitor}: cosi' nessun {@code instanceof}
     * e ogni nuovo tipo di {@link ClientRequest} viene rilevato a compile-time.
     *
     * <p>Il metodo e' {@code synchronized}: serializza le mutazioni del registry
     * delle partite e delle singole {@link MatchSession}. Il throughput non e'
     * critico (le partite attive in parallelo sono poche e ogni handler e'
     * leggero), in cambio si evita ogni race su stato condiviso.
     *
     * @param request la richiesta deserializzata; se {@code null} viene
     *                risposto un {@link ErrorMessage} e non viene fatto nulla
     * @param channel il canale del client che ha inviato la richiesta;
     *                catturato dal {@link Dispatcher} e usato per inviare
     *                eventuali risposte/errori
     */
    public synchronized void handleRequest(ClientRequest request, ClientChannel channel) {
        if (request == null) {
            channel.send(new ErrorMessage("Null request."));
            return;
        }
        try {
            request.accept(new Dispatcher(channel));
        } catch (Exception e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? "Unexpected network-server error."
                    : e.getMessage();
            channel.send(new ErrorMessage(message));
        }
    }

    /**
     * Gestisce una caduta di trasporto: il client non ha inviato
     * {@link DisconnectPlayerRequest} ma il livello sottostante (socket
     * chiuso, RMI unreachable) ha segnalato la fine della connessione.
     * Viene invocato direttamente dai trasporti, non passando dal
     * {@link ClientRequestVisitor}.
     *
     * <p>La logica:
     * <ol>
     *     <li>recupera il binding {@code (matchId, nickname)} memorizzato
     *         sul canale al momento del join;</li>
     *     <li>se il canale non era legato ad alcuna partita o lo era
     *         in modo incoerente, si limita a chiudere il canale ed esce;</li>
     *     <li>altrimenti delega a {@link MatchSession#handleDisconnect}
     *         con {@code transportDrop=true}, che applica la stessa
     *         logica del leave volontario ma chiude effettivamente il
     *         canale (qui non c'e' speranza di riusarlo).</li>
     * </ol>
     *
     * @param channel canale per cui il trasporto ha rilevato la chiusura
     */
    public synchronized void handleTransportDrop(ClientChannel channel) {
        String matchId = channel.getBoundMatchId();
        String nickname = channel.getBoundNickname();
        if (matchId == null || nickname == null || nickname.isBlank()) {
            channel.close();
            return;
        }
        MatchSession session = matchesById.get(matchId);
        if (session == null) {
            channel.close();
            return;
        }
        session.handleDisconnect(nickname, channel, true);
    }

    /**
     * Visitor che lega ogni richiesta in arrivo al metodo corretto della
     * MatchSession (o del registry, per le richieste globali), passando il
     * {@link ClientChannel} originario catturato in costruttore.
     */
    private final class Dispatcher implements ClientRequestVisitor {
        private final ClientChannel channel;

        /**
         * Cattura il canale del client che ha inviato la richiesta: lo
         * stesso canale verra' passato ai metodi handler della MatchSession
         * per inviare risposte o broadcast.
         */
        private Dispatcher(ClientChannel channel) {
            this.channel = channel;
        }

        @Override
        public void visit(CreateMatchRequest request) {
            handleCreateMatch(request, channel);
        }

        @Override
        public void visit(ListMatchesRequest request) {
            handleListMatches(channel);
        }

        @Override
        public void visit(AddPlayerToLobbyRequest request) {
            requireSession(request.matchId()).handleAddPlayer(request, channel);
        }

        @Override
        public void visit(SetExpectedPlayersRequest request) {
            requireSession(request.matchId()).handleSetExpectedPlayers(request, channel);
        }

        @Override
        public void visit(RemovePlayerFromLobbyRequest request) {
            requireSession(request.matchId()).handleRemoveFromLobby(request, channel);
        }

        @Override
        public void visit(PlaceTotemRequest request) {
            requireSession(request.matchId()).handlePlaceTotem(request, channel);
        }

        @Override
        public void visit(PickCardsRequest request) {
            requireSession(request.matchId()).handlePickCards(request, channel);
        }

        @Override
        public void visit(PickBonusCardRequest request) {
            requireSession(request.matchId()).handlePickBonusCard(request, channel);
        }

        @Override
        public void visit(DisconnectPlayerRequest request) {
            requireSession(request.matchId()).handleDisconnect(request.nickname(), channel, false);
        }
    }

    /** Richieste globali --------------------------------------------------- */

    /**
     * Crea una nuova partita su richiesta di un client.
     *
     * <p>Step:
     * <ol>
     *     <li>valida il nickname dell'host (non null/blank);</li>
     *     <li>genera un nuovo {@code matchId} univoco via {@link #nextMatchId};</li>
     *     <li>istanzia una {@link MatchSession} e la registra subito nel
     *         registry, cosi' una eventuale {@link ListMatchesRequest}
     *         concorrente la vede;</li>
     *     <li>delega a {@link MatchSession#handleHostCreateAndSetup} che
     *         aggiunge l'host alla lobby e imposta il numero di giocatori
     *         attesi in un'unica transizione (un solo broadcast finale).</li>
     * </ol>
     *
     * <p>Gestione degli errori: la creazione puo' fallire in due modi distinti
     * e il rollback e' diverso per ciascuno.
     * <ul>
     *     <li>Se l'host non e' nemmeno riuscito a unirsi (lobby vuota),
     *         la sessione e' inutilizzabile e viene rimossa dal registry.</li>
     *     <li>Se solo {@code setExpectedPlayers} ha fallito, l'host e' gia'
     *         in lobby: la partita resta viva, cosi' l'host puo' correggere
     *         il valore con un nuovo comando {@code players <N>}.</li>
     * </ul>
     * In entrambi i casi l'eccezione viene rilanciata, in modo che il
     * dispatcher esterno la converta in {@link ErrorMessage} verso l'host.
     *
     * @param request payload con nickname host e numero di giocatori attesi
     * @param channel canale del client host
     */
    private void handleCreateMatch(CreateMatchRequest request, ClientChannel channel) {
        String hostNickname = requireText(request.hostNickname(), "hostNickname");
        String matchId = nextMatchId();
        MatchSession session = new MatchSession(matchId);
        matchesById.put(matchId, session);
        try {
            session.handleHostCreateAndSetup(hostNickname, request.expectedPlayers(), channel);
        } catch (RuntimeException e) {
            // Two failure modes:
            //  - host couldn't even join: lobby is empty, the session is useless -> drop it.
            //  - only setExpectedPlayers failed: host is already in lobby, KEEP the match
            //    alive so they can fix it via `players <N>`. The error still propagates so
            //    the outer dispatcher surfaces it as ErrorMessage to the host.
            if (session.gameController.getLobbyPlayers().isEmpty()) {
                matchesById.remove(matchId);
            }
            throw e;
        }
    }

    /**
     * Costruisce e invia al solo richiedente un {@link MatchesListMessage}
     * con l'elenco delle partite in stato di lobby (non ancora partite).
     *
     * <p>Le partite gia' avviate vengono filtrate: non sono joinabili e
     * non interessano alla scena "Lista partite" del client. Per ciascuna
     * partita aperta viene incluso un {@link MatchInfoDTO} con
     * {@code matchId}, nickname dell'host, numero atteso di giocatori e
     * numero di giocatori attualmente in lobby.
     *
     * @param channel canale del client che ha richiesto la lista
     */
    private void handleListMatches(ClientChannel channel) {
        List<MatchInfoDTO> open = new ArrayList<>();
        for (MatchSession session : matchesById.values()) {
            GameController gc = session.gameController;
            if (gc.hasStarted()) continue;
            open.add(new MatchInfoDTO(
                    gc.getMatchId(),
                    gc.getHostNickname(),
                    gc.getExpectedPlayers(),
                    gc.getLobbyPlayers().size(),
                    false));
        }
        channel.send(new MatchesListMessage(open));
    }

    /**
     * Helper di lookup: cerca nel registry la {@link MatchSession} con
     * l'id indicato. Usato da ogni handler di richiesta "per-match" del
     * {@link Dispatcher} per ottenere la sessione su cui invocare l'handler.
     *
     * @param matchId id della partita richiesta dal client
     * @return la sessione corrispondente, mai {@code null}
     * @throws IllegalArgumentException se {@code matchId} e' nullo/blank
     *                                  o non corrisponde a nessuna partita
     *                                  attiva nel registry
     */
    private MatchSession requireSession(String matchId) {
        if (matchId == null || matchId.isBlank()) {
            throw new IllegalArgumentException("matchId is required for this request.");
        }
        MatchSession session = matchesById.get(matchId);
        if (session == null) {
            throw new IllegalArgumentException("Unknown match: " + matchId);
        }
        return session;
    }

    /**
     * Genera un identificativo univoco e leggibile per una nuova partita,
     * nella forma {@code M-<n>} con {@code n} progressivo monotono.
     * Si appoggia ad {@link AtomicLong} per essere sicuro anche al di fuori
     * della {@code synchronized} di {@link #handleRequest}.
     *
     * @return l'id della prossima partita, mai usato in precedenza
     */
    private String nextMatchId() {
        return "M-" + matchIdSeq.incrementAndGet();
    }

    /**
     * Helper di validazione per parametri testuali obbligatori. Centralizza
     * il controllo "non null e non blank" e produce un messaggio d'errore
     * uniforme che cita il nome del campo, cosi' nelle risposte di errore
     * al client si capisce immediatamente quale parametro mancava.
     *
     * @param value     valore ricevuto dal client
     * @param fieldName nome del campo, usato nel messaggio d'eccezione
     * @return {@code value} invariato, garantito non null e non blank
     * @throws IllegalArgumentException se {@code value} e' null o blank
     */
    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank.");
        }
        return value;
    }

    /**
     * Stato di una singola partita: GameController + VirtualView dedicati.
     * Tutta la logica per match resta isolata dalle altre partite.
     */
    private final class MatchSession {

        private final GameController gameController;
        private final VirtualView virtualView;
        private boolean observerAttached;

        /**
         * Crea una nuova sessione di partita: instanzia il {@link GameController}
         * con l'id indicato e la {@link VirtualView} dedicata. L'observer
         * sul model verra' agganciato solo a partita avviata
         * (vedi {@link #attachObserverIfNeeded}).
         */
        private MatchSession(String matchId) {
            this.gameController = new GameController(matchId);
            this.virtualView = new VirtualView(mapper);
            this.observerAttached = false;
        }

        /**
         * Aggiunge un giocatore (non host) alla lobby di questa partita.
         *
         * <p>Sequenza:
         * <ol>
         *     <li>memorizza lo stato pre-mossa di {@code hasStarted} per
         *         capire poi se l'aggiunta ha innescato l'avvio della partita
         *         (raggiungendo il numero atteso di giocatori);</li>
         *     <li>delega al {@link GameController} l'inserimento del nickname
         *         in lobby (puo' lanciare se duplicato/partita piena);</li>
         *     <li>collega canale e nickname tramite {@link #bind};</li>
         *     <li>conferma al solo richiedente con
         *         {@link MatchJoinedMessage} che riporta matchId e nickname;</li>
         *     <li>pubblica la transizione di stato a tutti i partecipanti
         *         via {@link #publishStateChange}.</li>
         * </ol>
         *
         * @param request payload con il nickname da aggiungere
         * @param channel canale del client che si sta unendo
         */
        private void handleAddPlayer(AddPlayerToLobbyRequest request, ClientChannel channel) {
            boolean wasStarted = gameController.hasStarted();
            gameController.addPlayerToLobby(request.nickname());
            bind(request.nickname(), channel);
            channel.send(new MatchJoinedMessage(gameController.getMatchId(), request.nickname()));
            publishStateChange(wasStarted);
        }

        /**
         * Variante usata da {@code handleCreateMatch}: aggiunge l'host e imposta
         * il numero di giocatori attesi in un'unica transizione, con un solo
         * broadcast finale. Senza questa coalescenza il client riceverebbe due
         * {@code LobbyStateMessage} consecutivi (uno con expected=0 e uno con
         * il valore richiesto), mostrando due volte la lobby.
         */
        private void handleHostCreateAndSetup(String hostNickname, int expectedPlayers, ClientChannel channel) {
            boolean wasStarted = gameController.hasStarted();
            gameController.addPlayerToLobby(hostNickname);
            bind(hostNickname, channel);
            channel.send(new MatchJoinedMessage(gameController.getMatchId(), hostNickname));
            RuntimeException pendingError = null;
            try {
                gameController.setExpectedPlayers(hostNickname, expectedPlayers);
            } catch (RuntimeException e) {
                pendingError = e;
            }
            publishStateChange(wasStarted);
            if (pendingError != null) {
                throw pendingError;
            }
        }

        /**
         * Aggiorna il numero di giocatori attesi per questa partita.
         * L'operazione e' consentita solo all'host: il
         * {@link GameController} verifica il vincolo e lancia se il
         * richiedente non e' l'host (o se il valore e' fuori range).
         *
         * <p>Se il nuovo valore coincide con il numero attuale di giocatori
         * in lobby, la partita parte: la transizione viene rilevata
         * confrontando {@code hasStarted} prima e dopo, e
         * {@link #publishStateChange} emette il {@link GameStartedMessage}
         * (vs un semplice {@link LobbyStateMessage}).
         *
         * @param request payload con nickname del richiedente e nuovo valore
         * @param channel canale del client richiedente, riallineato via
         *                {@link #bindIfKnown}
         */
        private void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientChannel channel) {
            bindIfKnown(request.requesterNickname(), channel);
            boolean wasStarted = gameController.hasStarted();
            gameController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
            publishStateChange(wasStarted);
        }

        /**
         * Gestisce un leave volontario dalla lobby (pre-game). Rimuove il
         * giocatore dal model, sgancia la sua VirtualView e libera il
         * binding del canale; il canale resta aperto perche' il client
         * tornera' a list/create/join sulla stessa connessione. Se la
         * lobby resta vuota, libera lo slot nel registry.
         */
        private void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientChannel channel) {
            bindIfKnown(request.nickname(), channel);
            gameController.removePlayerFromLobby(request.nickname());
            virtualView.unbind(request.nickname());
            unbindMatch(channel);
            // NB: il canale resta aperto. È un leave volontario: il client
            // userà la stessa connessione per tornare alla scena "lista partite"
            // e poter di nuovo list/create/join, senza dover riconnettersi.
            broadcastLobbyState();
            cleanupIfEmpty();
        }

        /**
         * Esegue la mossa "piazza totem": il giocatore sceglie una delle
         * offer tile disponibili (indicata da una lettera) e ci posa il
         * proprio totem, vincolando la pesca successiva.
         *
         * <p>La chiamata al {@link GameController} e' avvolta in un batch
         * della {@link VirtualView}: durante il batch le {@code notifyObservers}
         * emesse dal model non producono messaggi singoli, e a {@code endBatch}
         * viene inviato un unico {@link GameStateMessage} aggregato.
         * Senza batching, una sola mossa puo' generare 5-6 notifiche
         * (mosse, eventi, scoring) e altrettanti render lato client.
         *
         * <p>Il blocco {@code try/finally} garantisce che il batch venga
         * sempre chiuso anche se il controller solleva eccezione, evitando
         * di lasciare la VirtualView in stato "batch aperto" per sempre.
         *
         * @param request payload con nickname del giocatore e lettera della
         *                offer tile scelta
         * @param channel canale del giocatore, riallineato se gia' bindato
         */
        private void handlePlaceTotem(PlaceTotemRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            virtualView.beginBatch();
            try {
                gameController.placeTotem(request.playerNickname(), request.offerLetter());
            } finally {
                virtualView.endBatch();
            }
            broadcastGameStateAndMaybeEnd();
        }

        /**
         * Esegue la mossa "pesca carte": il giocatore consuma il proprio
         * totem dalla offer tile e prende le carte scelte. L'ordine degli
         * id nella lista <strong>e' significativo</strong>: alcune carte
         * influenzano altre in base alla sequenza (es. un Builder pescato
         * prima di una Building applica lo sconto; viceversa no).
         *
         * <p>Stesso schema di batching di {@link #handlePlaceTotem}: la
         * pesca tipicamente innesca scoring di eventi/edifici e quindi
         * molteplici notifiche del model, che vanno coalescate per evitare
         * render duplicati.
         *
         * @param request payload con nickname e lista ordinata degli id
         *                delle carte selezionate
         * @param channel canale del giocatore, riallineato se gia' bindato
         */
        private void handlePickCards(PickCardsRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            virtualView.beginBatch();
            try {
                gameController.pickCards(request.playerNickname(), request.selectedCardIds());
            } finally {
                virtualView.endBatch();
            }
            broadcastGameStateAndMaybeEnd();
        }

        /**
         * Esegue la scelta della carta bonus durante la
         * {@code BonusCardSelectionState}: il giocatore prende una sola carta
         * dal mazzetto bonus dell'era corrente.
         *
         * <p>Identica gestione di batching di {@link #handlePlaceTotem} e
         * {@link #handlePickCards}: la scelta puo' propagare nuovo scoring
         * e una transizione di stato del Game, quindi piu' notifiche.
         *
         * @param request payload con nickname e id della carta bonus scelta
         * @param channel canale del giocatore, riallineato se gia' bindato
         */
        private void handlePickBonusCard(PickBonusCardRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            virtualView.beginBatch();
            try {
                gameController.pickBonusCard(request.playerNickname(), request.bonusCardId());
            } finally {
                virtualView.endBatch();
            }
            broadcastGameStateAndMaybeEnd();
        }

        /**
         * Gestisce l'uscita di un giocatore da questa partita, sia che si
         * tratti di un leave volontario ({@code transportDrop=false},
         * arrivato come {@link DisconnectPlayerRequest}) sia di una caduta
         * di trasporto ({@code transportDrop=true}, segnalata da
         * {@link NetworkGameService#handleTransportDrop}).
         *
         * <p><strong>Pre-game (partita non ancora iniziata)</strong>:
         * <ul>
         *     <li>il giocatore viene rimosso dalla lobby (eccezioni
         *         silenziate: il fatto che non sia presente non e' un errore
         *         fatale in questo contesto);</li>
         *     <li>la {@link VirtualView} sgancia il binding del nickname;</li>
         *     <li>se la disconnessione e' di trasporto chiudiamo davvero il
         *         canale, altrimenti lo lasciamo aperto: e' un leave volontario,
         *         il client tornera' alla scena "Lista partite" sulla stessa
         *         connessione senza dover riconnettersi;</li>
         *     <li>si fa broadcast del nuovo stato di lobby e, se la lobby
         *         e' rimasta vuota, si libera lo slot dal registry.</li>
         * </ul>
         *
         * <p><strong>Mid-game</strong>: una disconnessione abbatte la partita
         * per tutti. Si invia un {@link MatchClosedMessage} a tutti i canali
         * ancora legati alla partita, si sgancia ogni canale dal matchId
         * <em>senza chiuderlo</em> (cosi' chi non si era disconnesso resta
         * connesso al server e puo' di nuovo list/create/join) e infine la
         * sessione viene rimossa dal registry.
         *
         * @param nickname      nickname del giocatore che esce
         * @param channel       canale del giocatore che ha inviato il leave
         *                      o per cui e' caduto il trasporto
         * @param transportDrop {@code true} se la chiamata viene da
         *                      {@link #handleTransportDrop}, {@code false}
         *                      per un leave esplicito del client
         */
        private void handleDisconnect(String nickname, ClientChannel channel, boolean transportDrop) {
            if (!gameController.hasStarted()) {
                try {
                    gameController.removePlayerFromLobby(nickname);
                } catch (Exception ignored) {
                }
                virtualView.unbind(nickname);
                unbindMatch(channel);
                if (transportDrop) {
                    // Trasporto morto davvero: chiudiamo per pulizia.
                    channel.close();
                }
                // Leave volontario pre-game: il canale resta vivo, il client
                // tornerà alla scena "lista partite" sulla stessa connessione.
                broadcastLobbyState();
                cleanupIfEmpty();
                return;
            }

            // Mid-game: il giocatore esce dal match.
            virtualView.unbind(nickname);
            unbindMatch(channel);
            if (transportDrop) {
                channel.close();
            }
            // Avvisiamo gli altri partecipanti che il match è abortito.
            virtualView.broadcast(new MatchClosedMessage(
                    "Player " + nickname + " disconnected. The match has been closed."));
            // ... e sganciamo TUTTI i canali superstiti dal match SENZA chiuderli:
            // chi ha fatto leave volontariamente e i restanti partecipanti
            // restano connessi al server e possono di nuovo list/create/join
            // dalla "situazione iniziale".
            for (ClientChannel other : virtualView.snapshotChannels()) {
                unbindMatch(other);
            }
            virtualView.unbindAllKeepingChannels();
            matchesById.remove(gameController.getMatchId());
        }

        /**
         * Pubblica lo stato giusto in base alla transizione avvenuta:
         * partita appena avviata → {@link GameStartedMessage} (porta lo stato iniziale);
         * partita in corso → {@link GameStateMessage};
         * ancora in lobby → {@link LobbyStateMessage}.
         *
         * <p>Allo start NON viene emesso un {@link GameStateMessage} aggiuntivo:
         * lo {@link GameStartedMessage} già contiene lo stato iniziale e tutte
         * le view lo rendono. Inviare entrambi causerebbe un doppio render.
         */
        private void publishStateChange(boolean wasStarted) {
            if (!wasStarted && gameController.hasStarted()) {
                attachObserverIfNeeded();
                GameStateDTO state = mapper.toGameState(gameController.getGame());
                virtualView.broadcast(new GameStartedMessage(state));
                maybeBroadcastEndGame(state);
            } else if (gameController.hasStarted()) {
                broadcastGameStateAndMaybeEnd();
            } else {
                broadcastLobbyState();
            }
        }

        /**
         * Costruisce un {@link LobbyStateDTO} a partire dallo stato attuale
         * del {@link GameController} (host, expected, lista giocatori) e lo
         * invia come {@link LobbyStateMessage} a tutti i canali registrati
         * nella {@link VirtualView}. Usato dopo ogni mutazione pre-game.
         */
        private void broadcastLobbyState() {
            LobbyStateDTO lobbyState = mapper.toLobbyState(gameController);
            virtualView.broadcast(new LobbyStateMessage(lobbyState));
        }

        /**
         * Variante "post-mossa" del broadcast: il {@link GameStateMessage}
         * lo ha gia' inviato la VirtualView come observer del model
         * (vedi commento interno). Qui ci limitiamo a verificare se la
         * partita si e' conclusa con l'ultima mossa e, in tal caso,
         * a far partire il flusso di fine partita.
         */
        private void broadcastGameStateAndMaybeEnd() {
            if (!gameController.hasStarted()) {
                return;
            }
            // NB: NON inviamo qui un GameStateMessage. La VirtualView è già
            // registrata come observer del Game (vedi attachObserverIfNeeded)
            // e il model emette notifyObservers() su ogni mutazione: lo stato
            // arriva ai client per quella via. Un broadcast esplicito qui
            // produrrebbe un secondo render identico subito dopo.
            GameStateDTO state = mapper.toGameState(gameController.getGame());
            maybeBroadcastEndGame(state);
        }

        /**
         * Se la partita risulta terminata, determina il vincitore, persiste
         * il risultato di tutti i giocatori sul database e invia a tutti i
         * canali un {@link EndGameMessage} con vincitore, stato finale,
         * classifica storica e posizioni dei giocatori. Infine rimuove la
         * partita dal registry e schedula la chiusura dei canali con un
         * grace period (vedi {@link #endGameCloser}).
         */
        private void maybeBroadcastEndGame(GameStateDTO state) {
            if (!gameController.getGame().isGameEnded()) {
                return;
            }
            // gameController.determineWinner() chiama Game.determineWinner() che
            // a sua volta fa notifyObservers(): senza il batch l'observer
            // produrrebbe un GameStateMessage identico a quello già emesso a
            // fine azione, duplicando il render del Game Over. Avvolgiamo la
            // chiamata in un batch e lo chiudiamo SENZA emettere: lo stato
            // finale viene comunque consegnato dentro l'EndGameMessage qui sotto.
            Player winner;
            virtualView.beginBatch();
            try {
                winner = gameController.determineWinner();
            } finally {
                virtualView.endBatch(false);
            }

            List<Player> players = gameController.getGame().getPlayers();
            int numPlayers = players.size();
            List<MatchResultDao.PlayerResult> results = new ArrayList<>(numPlayers);
            Map<String, Integer> finalScores = new HashMap<>(numPlayers * 2);
            for (Player p : players) {
                int score = p.finalPP();
                results.add(new MatchResultDao.PlayerResult(p.getNickname(), score));
                finalScores.put(p.getNickname(), score);
            }

            List<LeaderboardEntryDTO> leaderboard = List.of();
            Map<String, Integer> positions = Map.of();
            try {
                matchResultDao.saveMatch(results, numPlayers);
                leaderboard = loadLeaderboard(numPlayers);
                positions = computePositions(numPlayers, finalScores);
            } catch (SQLException e) {
                System.err.println("[match " + gameController.getMatchId()
                        + "] Persistenza classifica non disponibile: " + e.getMessage());
            }

            virtualView.broadcast(new EndGameMessage(
                    mapper.toWinnerDTO(winner), state, leaderboard, positions));
            // Drop the match from the registry right away so no further requests can be
            // routed to it, but defer the channel close: see endGameCloser javadoc.
            matchesById.remove(gameController.getMatchId());
            VirtualView viewToClose = virtualView;
            endGameCloser.schedule(viewToClose::closeAll, END_GAME_CLOSE_DELAY_MS, TimeUnit.MILLISECONDS);
        }

        /**
         * Carica dal DB la classifica storica filtrata per dimensione
         * partita: i punteggi di una partita a 2 giocatori non sono
         * confrontabili con quelli di una a 4, per questo la leaderboard
         * e' indicizzata per {@code numPlayers}.
         *
         * <p>Trasforma le {@link MatchResultDao.RankRow} restituite dal DAO
         * in {@link LeaderboardEntryDTO} pronti per la serializzazione.
         *
         * @param numPlayers numero di giocatori della categoria di partite
         * @return classifica storica, ordinata dal DAO (top punteggi prima)
         * @throws SQLException se la query al DAO fallisce
         */
        private List<LeaderboardEntryDTO> loadLeaderboard(int numPlayers) throws SQLException {
            List<MatchResultDao.RankRow> rows = matchResultDao.getLeaderboard(numPlayers);
            List<LeaderboardEntryDTO> out = new ArrayList<>(rows.size());
            for (MatchResultDao.RankRow r : rows) {
                out.add(new LeaderboardEntryDTO(r.nickname(), r.score(), r.endDate(), numPlayers));
            }
            return out;
        }

        /**
         * Per ogni giocatore della partita appena terminata calcola la
         * posizione (1-based) che il suo punteggio finale occupa nella
         * classifica storica delle partite con lo stesso numero di
         * giocatori. Utile per mostrare al client una riga del tipo
         * "Sei 12esimo nel ranking partite a 3 giocatori".
         *
         * @param numPlayers  dimensione della partita (chiave del ranking)
         * @param finalScores mappa nickname -> punteggio finale
         * @return mappa nickname -> posizione nel ranking storico
         * @throws SQLException se una delle query al DAO fallisce
         */
        private Map<String, Integer> computePositions(int numPlayers,
                                                      Map<String, Integer> finalScores) throws SQLException {
            Map<String, Integer> positions = new HashMap<>(finalScores.size() * 2);
            for (Map.Entry<String, Integer> e : finalScores.entrySet()) {
                positions.put(e.getKey(),
                        matchResultDao.getPosition(numPlayers, e.getValue()));
            }
            return positions;
        }

        /**
         * Stabilisce (o aggiorna) il legame tra un nickname e un canale per
         * questa partita: la {@link VirtualView} memorizza il canale come
         * destinatario dei messaggi per quel nickname, e il canale memorizza
         * il {@code matchId} di appartenenza (usato in seguito da
         * {@link #handleTransportDrop} per risalire alla sessione).
         *
         * <p>{@code bindOrReplace} e' idempotente: chiamare {@code bind}
         * piu' volte con lo stesso canale e' sicuro e serve in caso di
         * riconnessione.
         */
        private void bind(String nickname, ClientChannel channel) {
            virtualView.bindOrReplace(nickname, channel);
            channel.setBoundMatchId(gameController.getMatchId());
        }

        /**
         * Variante "soft" di {@link #bind} usata dagli handler di mossa.
         * Se il nickname e' gia' noto alla {@link VirtualView}, aggiorna il
         * canale associato (caso utile quando una richiesta arriva da un
         * canale diverso da quello di join, es. una riconnessione). Se il
         * nickname non e' bindato non fa nulla: non e' compito di un handler
         * di mossa creare un nuovo binding.
         *
         * <p>Tollera nickname nulli/blank ritornando in silenzio: la
         * validazione vera e' a carico del {@link GameController}.
         */
        private void bindIfKnown(String nickname, ClientChannel channel) {
            if (nickname == null || nickname.isBlank()) {
                return;
            }
            if (virtualView.isBound(nickname)) {
                bind(nickname, channel);
            }
        }

        /**
         * Rimuove dal canale l'eventuale binding al {@code matchId} di
         * questa sessione, ma solo se l'id corrisponde davvero: cosi'
         * evitiamo di sganciare un canale che nel frattempo fosse stato
         * riassegnato a un'altra partita. Dopo questa operazione il client
         * risulta non piu' "iscritto" a questa partita, pur restando
         * collegato al server (il canale stesso non viene chiuso qui).
         */
        private void unbindMatch(ClientChannel channel) {
            if (Objects.equals(channel.getBoundMatchId(), gameController.getMatchId())) {
                channel.setBoundMatchId(null);
            }
        }

        /**
         * Garbage-collection delle lobby vuote: se la partita non e'
         * ancora iniziata e l'ultimo giocatore se ne e' andato, la
         * sessione e' inutile e viene rimossa dal registry. Senza questo,
         * gli id "morti" continuerebbero a comparire nella lista partite
         * fino al riavvio del server.
         */
        private void cleanupIfEmpty() {
            if (!gameController.hasStarted() && gameController.getLobbyPlayers().isEmpty()) {
                matchesById.remove(gameController.getMatchId());
            }
        }

        /**
         * Aggancia la {@link VirtualView} al {@code Game} come observer, ma
         * una sola volta nella vita della sessione (idempotente): il flag
         * {@code observerAttached} evita iscrizioni multiple, che
         * causerebbero la spedizione di N messaggi identici per ogni
         * mutazione del model.
         *
         * <p>L'aggancio avviene solo dopo lo start: prima dell'avvio
         * {@code gameController.getGame()} puo' essere {@code null} e
         * comunque non c'e' uno stato di gioco da osservare. Da questo
         * momento ogni {@code notifyObservers} del model si trasforma in un
         * {@link GameStateMessage} broadcast (eventualmente coalescato dal
         * batching, vedi {@link #handlePlaceTotem}).
         */
        private void attachObserverIfNeeded() {
            if (observerAttached || gameController.getGame() == null) {
                return;
            }
            gameController.getGame().addObserver(virtualView);
            observerAttached = true;
        }
    }
}
