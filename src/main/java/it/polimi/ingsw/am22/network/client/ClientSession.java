package it.polimi.ingsw.am22.network.client;

import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

import java.util.Objects;

/**
 * Sessione client: tiene insieme {@link ObservableServerConnection} e
 * {@link ClientController}, gestisce un unico {@link ClientUpdateHandler}
 * corrente (la view attiva) e memorizza lo "snapshot" più recente di lobby
 * e gioco ricevuti dal server.
 *
 * <p>Motivazione dello snapshot: i messaggi possono arrivare sul reader thread
 * (socket) o sul thread RMI prima che la view abbia avuto il tempo di
 * registrarsi. Tenendo l'ultimo {@code LobbyStateDTO}/{@code GameStateDTO}
 * possiamo "replay"are l'ultimo stato quando la nuova view si attacca.
 *
 * <p>Pattern MVC: questa classe è un elemento del lato Controller del client
 * (insieme a {@link ClientController}). La View vi si registra come
 * {@link ClientUpdateHandler}; il Model remoto è rappresentato dai DTO.
 */
public final class ClientSession {

    private final ObservableServerConnection connection;
    private final ClientController clientController;

    /** Handler della view attualmente attiva (può cambiare a ogni switch di schermata). */
    private volatile ClientUpdateHandler currentHandler;

    /** Ultimo stato lobby ricevuto (per replay sulla view appena attaccata). */
    private volatile LobbyStateDTO latestLobbyState;

    /** Ultimo stato di gioco ricevuto (per replay sulla view appena attaccata). */
    private volatile GameStateDTO latestGameState;

    /** Diventa {@code true} non appena arriva {@link GameStartedMessage}. */
    private volatile boolean gameStarted;

    /**
     * Crea la sessione e registra internamente un dispatcher che:
     * <ul>
     *     <li>aggiorna gli snapshot;</li>
     *     <li>inoltra il messaggio all'handler corrente (se presente).</li>
     * </ul>
     *
     * @param connection connessione già aperta verso il server
     */
    public ClientSession(ObservableServerConnection connection) {
        this.connection = Objects.requireNonNull(connection, "connection cannot be null");
        this.clientController = new ClientController(connection);
        // Il dispatcher interno è sempre attivo: filtra gli snapshot e poi
        // delega alla view corrente. In questo modo i messaggi non si
        // perdono se arrivano prima che la view sia pronta.
        this.connection.setClientUpdateHandler(new InternalDispatcher());
    }

    /**
     * @return il controller che la view usa per inviare comandi al server
     */
    public ClientController getClientController() {
        return clientController;
    }

    /** @return nickname locale (può essere {@code null} prima del join). */
    public String getLocalNickname() {
        return clientController.getNickname();
    }

    /** @return ultimo stato lobby noto, o {@code null} se mai ricevuto. */
    public LobbyStateDTO getLatestLobbyState() {
        return latestLobbyState;
    }

    /** @return ultimo stato di gioco noto, o {@code null} se mai ricevuto. */
    public GameStateDTO getLatestGameState() {
        return latestGameState;
    }

    /** @return true se il server ha già notificato {@code GameStartedMessage}. */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Registra l'handler della view attiva. Se esiste già un'istantanea
     * di lobby o gioco, viene replayata subito all'handler in modo che la
     * schermata parta già sincronizzata.
     *
     * @param handler nuovo handler attivo (può essere {@code null} per disattivarlo)
     */
    public void setHandler(ClientUpdateHandler handler) {
        this.currentHandler = handler;
        if (handler == null) {
            return;
        }
        // Replay: chi si è appena attaccato riceve il messaggio più pertinente
        // per ricostruire il proprio stato iniziale.
        if (gameStarted && latestGameState != null) {
            handler.onServerMessage(new GameStateMessage(latestGameState));
        } else if (latestLobbyState != null) {
            handler.onServerMessage(new LobbyStateMessage(latestLobbyState));
        }
    }

    /**
     * Chiude la connessione in modo pulito.
     *
     * @param notifyServer se {@code true} invia prima una disconnect al server
     */
    public void close(boolean notifyServer) {
        try {
            if (notifyServer && clientController.hasJoinedLobby()) {
                clientController.disconnect();
            }
        } catch (RuntimeException ignored) {
            // Se la connessione è già caduta, ignoriamo: l'importante è chiudere.
        }
        connection.close();
    }

    /**
     * Dispatcher interno: è sempre registrato sulla connection, intercetta
     * i messaggi prima della view e aggiorna gli snapshot.
     */
    private final class InternalDispatcher implements ClientUpdateHandler {

        @Override
        public void onServerMessage(ServerMessage message) {
            // Aggiornamento snapshot prima di inoltrare: dispatch polimorfo via visitor.
            message.accept(new ServerMessageVisitor() {
                @Override public void visit(LobbyStateMessage m) { latestLobbyState = m.lobbyState(); }
                @Override public void visit(GameStartedMessage m) { gameStarted = true; latestGameState = m.initialGameState(); }
                @Override public void visit(GameStateMessage m) { latestGameState = m.gameState(); }
                @Override public void visit(EndGameMessage m) { latestGameState = m.finalGameState(); }
                @Override public void visit(MatchClosedMessage m) {}
                @Override public void visit(ErrorMessage m) {}
                @Override public void visit(InfoMessage m) {}
            });
            ClientUpdateHandler handler = currentHandler;
            if (handler != null) {
                handler.onServerMessage(message);
            }
        }

        @Override
        public void onConnectionClosed(Throwable cause) {
            ClientUpdateHandler handler = currentHandler;
            if (handler != null) {
                handler.onConnectionClosed(cause);
            }
        }
    }
}
