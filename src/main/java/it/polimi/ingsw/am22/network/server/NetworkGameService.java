package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.network.common.message.ClientRequest;
import it.polimi.ingsw.am22.network.common.message.response.*;
import it.polimi.ingsw.am22.network.common.message.request.*;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;

public class NetworkGameService {
    private final GameController gameController;
    private final VirtualView virtualView;
    private final ModelDtoMapper mapper;

    public NetworkGameService(GameController gameController) {
        this.gameController = gameController;
        this.virtualView = new VirtualView();
        this.mapper = new ModelDtoMapper();
    }

    public VirtualView getVirtualView() {
        return virtualView;
    }

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

    public synchronized void handleTransportDrop(ClientChannel channel) {
        String nickname = channel.getBoundNickname();
        if (nickname == null || nickname.isBlank()) {
            channel.close();
            return;
        }
        handleDisconnect(nickname, channel, true);
    }

    private void handleAddPlayer(AddPlayerToLobbyRequest request, ClientChannel channel) {
        boolean wasStarted = gameController.hasStarted();
        gameController.addPlayerToLobby(request.nickname());
        virtualView.bindOrReplace(request.nickname(), channel);
        publishStateChange(wasStarted);
    }

    private void handleSetExpectedPlayers(SetExpectedPlayersRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.requesterNickname(), channel);
        boolean wasStarted = gameController.hasStarted();
        gameController.setExpectedPlayers(request.requesterNickname(), request.expectedPlayers());
        publishStateChange(wasStarted);
    }

    private void handleRemoveFromLobby(RemovePlayerFromLobbyRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.nickname(), channel);
        gameController.removePlayerFromLobby(request.nickname());
        virtualView.unbind(request.nickname());
        channel.close();
        broadcastLobbyState();
    }

    private void handlePlaceTotem(PlaceTotemRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.placeTotem(request.playerNickname(), request.offerLetter());
        broadcastGameStateAndMaybeEnd();
    }

    private void handlePickCards(PickCardsRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.pickCards(request.playerNickname(), request.selectedCardIds());
        broadcastGameStateAndMaybeEnd();
    }

    private void handlePickBonusCard(PickBonusCardRequest request, ClientChannel channel) {
        bindExistingNicknameIfPresent(request.playerNickname(), channel);
        gameController.pickBonusCard(request.playerNickname(), request.bonusCardId());
        broadcastGameStateAndMaybeEnd();
    }

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
    }

    private void bindExistingNicknameIfPresent(String nickname, ClientChannel channel) {
        if (nickname == null || nickname.isBlank()) {
            return;
        }
        if (virtualView.isBound(nickname)) {
            virtualView.bindOrReplace(nickname, channel);
        }
    }
}
