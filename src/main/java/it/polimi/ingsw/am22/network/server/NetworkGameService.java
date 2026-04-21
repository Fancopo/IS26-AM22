package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.response.*;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;

/**
 * Cuore del layer di rete lato server.
 *
 * Riceve le {@link ClientRequest} (indipendentemente dal trasporto Socket o RMI),
 * le dispatcha ai metodi del {@link GameController} e ritrasmette
 * tramite {@link VirtualView} gli aggiornamenti di stato a tutti i client.
 * Tutti i metodi pubblici che modificano lo stato sono {@code synchronized}
 * per serializzare richieste concorrenti provenienti da thread diversi
 * (worker socket + thread RMI).
 */
public class NetworkGameService {
    private final GameController gameController;
    private final VirtualView virtualView;
    private final ModelDtoMapper mapper;

    /**
     * Crea il servizio di rete collegato a uno specifico {@link GameController}.
     * Inizializza internamente una {@link VirtualView} vuota e un mapper per i DTO.
     *
     * @param gameController controller di gioco
     */
    public NetworkGameService(GameController gameController) {
        this.gameController = gameController;
        this.virtualView = new VirtualView();
        this.mapper = new ModelDtoMapper();
    }

    /** @return la VirtualView usata per il broadcast dei messaggi. */
    public VirtualView getVirtualView() {
        return virtualView;
    }

    /**
     * Gestisce una richiesta proveniente da un client dispatchandola al
     * metodo specifico in base al tipo. Eventuali eccezioni vengono
     * convertite in {@link ErrorMessage} inviati al solo mittente.
     *
     * @param request richiesta da elaborare
     * @param channel canale del client che ha inviato la richiesta
     */
    public synchronized void handleRequest(ClientRequest request, ClientChannel channel) {
        try {
            if (request instanceof AddPlayerToLobbyRequest addRequest) {
                handleAddPlayer(addRequest, channel);
                return;
            }
            if (request instanceof SetExpectedPlayersRequest expectedPlayersRequest) {
                handleSetExpectedPlayers(expectedPlayersRequest, channel);
                return;
            }
            if (request instanceof RemovePlayerFromLobbyRequest removeRequest) {
                handleRemoveFromLobby(removeRequest, channel);
                return;
            }
            if (request instanceof PlaceTotemRequest placeRequest) {
                handlePlaceTotem(placeRequest, channel);
                return;
            }
            if (request instanceof PickCardsRequest pickCardsRequest) {
                handlePickCards(pickCardsRequest, channel);
                return;
            }
            if (request instanceof PickBonusCardRequest pickBonusCardRequest) {
                handlePickBonusCard(pickBonusCardRequest, channel);
                return;
            }
            if (request instanceof DisconnectPlayerRequest disconnectRequest) {
                handleDisconnect(disconnectRequest.nickname(), channel, false);
                return;
            }
            channel.send(new ErrorMessage("Unsupported request type."));
        } catch (Exception e) {
            String message = e.getMessage() == null || e.getMessage().isBlank()
                    ? "Unexpected network-server error."
                    : e.getMessage();
            channel.send(new ErrorMessage(message));
        }
    }

    /**
     * Gestisce una caduta di trasporto (disconnessione non richiesta).
     * Invocato da {@link SocketClientHandler} quando lo stream chiude con errore.
     *
     * @param channel canale che ha perso il collegamento
     */
    public synchronized void handleTransportDrop(ClientChannel channel) {
        String nickname = channel.getBoundNickname();
        if (nickname == null || nickname.isBlank()) {
            channel.close();
            return;
        }
        handleDisconnect(nickname, channel, true);
    }

    /** Aggiunge un giocatore alla lobby, lega il canale al nickname e pubblica lo stato. */
    private void handleAddPlayer(AddPlayerToLobbyRequest request, ClientChannel channel) {
        boolean wasStarted = gameController.hasStarted();
        gameController.addPlayerToLobby(request.nickname());
        virtualView.bindOrReplace(request.nickname(), channel);
        publishStateChange(wasStarted);
    }

    /** Imposta il numero di giocatori attesi; se la partita parte, broadcast di avvio. */
    private void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.requesterNickname(), channel);
        boolean wasStarted = gameController.hasStarted();
        gameController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
        publishStateChange(wasStarted);
    }

    /** Rimuove il giocatore dalla lobby, chiude il canale, broadcast dello stato lobby. */
    private void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.nickname(), channel);
        gameController.removePlayerFromLobby(request.nickname());
        virtualView.unbind(request.nickname());
        channel.close();
        broadcastLobbyState();
    }

    /** Delega al controller e broadcasta il nuovo stato di gioco. */
    private void handlePlaceTotem(PlaceTotemRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.placeTotem(request.playerNickname(), request.offerLetter());
        broadcastGameStateAndMaybeEnd();
    }

    /** Delega al controller e broadcasta il nuovo stato di gioco. */
    private void handlePickCards(PickCardsRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.pickCards(request.playerNickname(), request.selectedCardIds());
        broadcastGameStateAndMaybeEnd();
    }

    /** Delega al controller e broadcasta il nuovo stato di gioco. */
    private void handlePickBonusCard(PickBonusCardRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.pickBonusCard(request.playerNickname(), request.bonusCardId());
        broadcastGameStateAndMaybeEnd();
    }

    /**
     * Gestisce la disconnessione di un giocatore, sia volontaria
     * ({@code transportDrop=false}) sia per caduta di trasporto
     * ({@code transportDrop=true}). Se la partita è in corso, chiude
     * la partita per tutti e notifica con {@link MatchClosedMessage}.
     */
    private void handleDisconnect(String nickname, ClientChannel channel, boolean transportDrop) {
        if (!gameController.hasStarted()) {
            try {
                gameController.removePlayerFromLobby(nickname);
            } catch (Exception ignored) {
            }
            virtualView.unbind(nickname);
            channel.close();
            broadcastLobbyState();
            return;
        }

        virtualView.unbind(nickname);
        channel.close();
        virtualView.broadcast(new MatchClosedMessage(
                "Player " + nickname + " disconnected. The match has been closed."
        ));
        virtualView.closeAll();
        if (!transportDrop) {
            virtualView.broadcast(new InfoMessage("Disconnected: " + nickname));
        }
    }

    /**
     * Pubblica lo stato giusto in base alla transizione avvenuta:
     * partita appena avviata → {@link GameStartedMessage} + {@link GameStateMessage};
     * partita in corso → solo {@link GameStateMessage};
     * ancora in lobby → {@link LobbyStateMessage}.
     */
    private void publishStateChange(boolean wasStarted) {
        if (!wasStarted && gameController.hasStarted()) {
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

    /** Broadcast dello stato di lobby corrente. */
    private void broadcastLobbyState() {
        LobbyStateDTO lobbyState = mapper.toLobbyState(gameController);
        virtualView.broadcast(new LobbyStateMessage(lobbyState));
    }

    /** Broadcast dello stato di gioco corrente e, se la partita è finita, notifica anche il vincitore. */
    private void broadcastGameStateAndMaybeEnd() {
        if (!gameController.hasStarted()) {
            return;
        }
        GameStateDTO state = mapper.toGameState(gameController.getGame());
        virtualView.broadcast(new GameStateMessage(state));
        maybeBroadcastEndGame(state);
    }

    /** Se la partita è terminata, invia {@link EndGameMessage} con il vincitore. */
    private void maybeBroadcastEndGame(GameStateDTO state) {
        if (!gameController.getGame().isGameEnded()) {
            return;
        }
        Player winner = gameController.determineWinner();
        virtualView.broadcast(new EndGameMessage(mapper.toWinnerDTO(winner), state));
    }

    /**
     * Se il nickname è già registrato nella VirtualView, aggiorna il suo canale
     * con quello fornito (utile in caso di riconnessione o richiesta da canale diverso).
     */
    private void bindExistingNicknameIfPresent(String nickname, ClientChannel channel) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        if (virtualView.isBound(nickname)) {
            virtualView.bindOrReplace(nickname, channel);
        }
    }
}
