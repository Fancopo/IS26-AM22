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
     * Gestisce una richiesta proveniente da un client dispatchandola al
     * metodo specifico in base al tipo. Eventuali eccezioni vengono
     * convertite in {@link ErrorMessage} inviati al solo mittente.
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
     * Gestisce una caduta di trasporto (disconnessione non richiesta).
     * Invocato dai trasporti quando lo stream chiude con errore.
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

    /** Creates a new match, adds the host to the lobby and notifies them. */
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

    /** Risponde al solo richiedente con la lista delle partite ancora aperte. */
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

    /** Recupera la sessione richiesta o solleva eccezione se inesistente. */
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

    /** Genera un id leggibile per la prossima partita. */
    private String nextMatchId() {
        return "M-" + matchIdSeq.incrementAndGet();
    }

    /**
     * Helper di validazione per parametri testuali: lancia
     * {@link IllegalArgumentException} se la stringa e' null o blank,
     * altrimenti la restituisce invariata.
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

        /** Aggiunge un giocatore alla lobby di questa partita. */
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
         * Imposta il numero di giocatori attesi per questa partita
         * (operazione consentita solo all'host) e ribroadcasta lo stato.
         * Se il valore raggiunto fa partire la partita, viene emesso anche
         * il {@link GameStartedMessage}.
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
         * Esegue la mossa di piazzamento del totem sulla tessera offerta
         * scelta. La chiamata al GameController e' avvolta in un batch della
         * VirtualView: tutte le notifiche emesse dal model vengono
         * coalescenziate in un unico {@link GameStateMessage} a fine batch.
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
         * Esegue la scelta delle carte da pescare. L'ordine degli id e'
         * significativo (es. Builder->Building applica lo sconto, viceversa no).
         * Batchato come {@link #handlePlaceTotem} per evitare doppi render.
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
         * Esegue la scelta della carta bonus durante la bonus phase.
         * Batchato come gli altri handler di mossa.
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
         * Gestisce la disconnessione di un giocatore di questa partita,
         * sia volontaria ({@code transportDrop=false}) sia per caduta di
         * trasporto ({@code transportDrop=true}). A partita avviata, la
         * disconnessione abbatte la partita per tutti.
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
         * Mappa lo stato corrente della lobby in un DTO e lo invia a tutti
         * i giocatori legati alla VirtualView di questa partita.
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
         * Carica dal DB la classifica storica per partite con il numero di
         * giocatori indicato e la converte in lista di {@link LeaderboardEntryDTO}
         * da spedire al client.
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
         * Calcola, per ogni giocatore della partita appena terminata, la
         * posizione che il suo punteggio finale occupa nella classifica
         * storica (interrogando il DAO con {@code getPosition}).
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

        /** Lega il canale al nickname e al matchId di questa sessione. */
        private void bind(String nickname, ClientChannel channel) {
            virtualView.bindOrReplace(nickname, channel);
            channel.setBoundMatchId(gameController.getMatchId());
        }

        /**
         * Se il nickname è già registrato nella VirtualView aggiorna il suo
         * canale (utile in caso di riconnessione o richiesta da canale diverso).
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
         * Rimuove dal canale il binding al matchId di questa sessione (solo
         * se corrisponde): cosi' il client non risulta piu' "iscritto" alla
         * partita, pur restando connesso al server.
         */
        private void unbindMatch(ClientChannel channel) {
            if (Objects.equals(channel.getBoundMatchId(), gameController.getMatchId())) {
                channel.setBoundMatchId(null);
            }
        }

        /**
         * Se la partita non è ancora iniziata e nessuno è più in lobby, libera
         * lo slot dal registry — evita che id "morti" inquinino la lista.
         */
        private void cleanupIfEmpty() {
            if (!gameController.hasStarted() && gameController.getLobbyPlayers().isEmpty()) {
                matchesById.remove(gameController.getMatchId());
            }
        }

        /**
         * Aggancia la VirtualView al Game come observer, una sola volta:
         * dal primo agganciamento ogni mutazione del model produce un
         * {@code GameStateMessage} broadcast.
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
