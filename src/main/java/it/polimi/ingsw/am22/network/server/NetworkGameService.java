package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.ClientRequestVisitor;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.common.message.response.*;

import java.util.ArrayList;
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

    public NetworkGameService() {
        this.mapper = new ModelDtoMapper();
        this.matchesById = new ConcurrentHashMap<>();
        this.matchIdSeq = new AtomicLong();
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

    /** Crea una nuova partita, aggiunge l'host alla lobby e lo notifica. */
    private void handleCreateMatch(CreateMatchRequest request, ClientChannel channel) {
        String hostNickname = requireText(request.hostNickname(), "hostNickname");
        String matchId = nextMatchId();
        MatchSession session = new MatchSession(matchId);
        matchesById.put(matchId, session);
        try {
            session.handleAddPlayer(new AddPlayerToLobbyRequest(matchId, hostNickname), channel);
            // L'host imposta subito anche il numero di giocatori attesi.
            session.handleSetExpectedPlayers(
                    new SetExpectedPlayersRequest(matchId, hostNickname, request.expectedPlayers()),
                    channel);
        } catch (RuntimeException e) {
            // Se la creazione fallisce a metà, libera lo slot.
            matchesById.remove(matchId);
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

        private void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientChannel channel) {
            bindIfKnown(request.requesterNickname(), channel);
            boolean wasStarted = gameController.hasStarted();
            gameController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
            publishStateChange(wasStarted);
        }

        private void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientChannel channel) {
            bindIfKnown(request.nickname(), channel);
            gameController.removePlayerFromLobby(request.nickname());
            virtualView.unbind(request.nickname());
            unbindMatch(channel);
            channel.close();
            broadcastLobbyState();
            cleanupIfEmpty();
        }

        private void handlePlaceTotem(PlaceTotemRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            gameController.placeTotem(request.playerNickname(), request.offerLetter());
            broadcastGameStateAndMaybeEnd();
        }

        private void handlePickCards(PickCardsRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            gameController.pickCards(request.playerNickname(), request.selectedCardIds());
            broadcastGameStateAndMaybeEnd();
        }

        private void handlePickBonusCard(PickBonusCardRequest request, ClientChannel channel) {
            bindIfKnown(request.playerNickname(), channel);
            gameController.pickBonusCard(request.playerNickname(), request.bonusCardId());
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
                channel.close();
                broadcastLobbyState();
                cleanupIfEmpty();
                return;
            }

            virtualView.unbind(nickname);
            unbindMatch(channel);
            channel.close();
            virtualView.broadcast(new MatchClosedMessage(
                    "Player " + nickname + " disconnected. The match has been closed."));
            virtualView.closeAll();
            if (!transportDrop) {
                virtualView.broadcast(new InfoMessage("Disconnected: " + nickname));
            }
            matchesById.remove(gameController.getMatchId());
        }

        /**
         * Pubblica lo stato giusto in base alla transizione avvenuta:
         * partita appena avviata → {@link GameStartedMessage} + {@link GameStateMessage};
         * partita in corso → solo {@link GameStateMessage};
         * ancora in lobby → {@link LobbyStateMessage}.
         */
        private void publishStateChange(boolean wasStarted) {
            if (!wasStarted && gameController.hasStarted()) {
                attachObserverIfNeeded();
                GameStateDTO state = mapper.toGameState(gameController.getGame());
                virtualView.broadcast(new GameStartedMessage(state));
                virtualView.broadcast(new GameStateMessage(state));
                maybeBroadcastEndGame(state);
            } else if (gameController.hasStarted()) {
                broadcastGameStateAndMaybeEnd();
            } else {
                broadcastLobbyState();
            }
        }

        private void broadcastLobbyState() {
            LobbyStateDTO lobbyState = mapper.toLobbyState(gameController);
            virtualView.broadcast(new LobbyStateMessage(lobbyState));
        }

        private void broadcastGameStateAndMaybeEnd() {
            if (!gameController.hasStarted()) {
                return;
            }
            GameStateDTO state = mapper.toGameState(gameController.getGame());
            virtualView.broadcast(new GameStateMessage(state));
            maybeBroadcastEndGame(state);
        }

        private void maybeBroadcastEndGame(GameStateDTO state) {
            if (!gameController.getGame().isGameEnded()) {
                return;
            }
            Player winner = gameController.determineWinner();
            virtualView.broadcast(new EndGameMessage(mapper.toWinnerDTO(winner), state));
            // Drop the match from the registry right away so no further requests can be
            // routed to it, but defer the channel close: see endGameCloser javadoc.
            matchesById.remove(gameController.getMatchId());
            VirtualView viewToClose = virtualView;
            endGameCloser.schedule(viewToClose::closeAll, END_GAME_CLOSE_DELAY_MS, TimeUnit.MILLISECONDS);
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
