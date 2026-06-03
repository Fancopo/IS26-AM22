package it.polimi.ingsw.am22.network.protocol;

import it.polimi.ingsw.am22.controller.server.MatchController;
import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.network.protocol.dto.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Single point where the network layer reads the model. Builds the DTOs
 * (GameState/LobbyState/Winner/Player/Card/OfferTile/TurnSlot) from Game and MatchController.
 */
public class ModelDtoMapper {

    public LobbyStateDTO toLobbyState(MatchController matchController) {
        List<LobbyPlayerDTO> players = matchController.getLobbyPlayers().stream()
                .map(player -> new LobbyPlayerDTO(
                        player.getNickname(),
                        Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                        player.getNickname().equals(matchController.getHostNickname())
                ))
                .toList();

        return new LobbyStateDTO(
                matchController.getMatchId(),
                matchController.getHostNickname(),
                matchController.getExpectedPlayers(),
                matchController.hasStarted(),
                players
        );
    }

    public TotemSelectionStateDTO toTotemSelectionState(MatchController matchController) {
        List<Player> lobby = matchController.getLobbyPlayers();
        List<TotemOptionDTO> options = matchController.getTotemPalette().stream()
                .map(color -> new TotemOptionDTO(color, ownerOf(color, lobby)))
                .toList();
        return new TotemSelectionStateDTO(options, matchController.getCurrentTotemChooser());
    }

    /** Nickname of the player whose totem has this color, or null if free. */
    private String ownerOf(String color, List<Player> lobby) {
        return lobby.stream()
                .filter(p -> p.getTotem() != null && p.getTotem().getColor().equalsIgnoreCase(color))
                .map(Player::getNickname)
                .findFirst()
                .orElse(null);
    }

    public GameStateDTO toGameState(Game game) {
        Board board = game.getBoard();
        Player activePlayer = game.getActivePlayer();

        List<PlayerDTO> players = game.getPlayers().stream()
                .map(player -> toPlayerDTO(player, activePlayer))
                .toList();

        List<CardDTO> upperRow = board.getUpperRow().stream().map(this::toCardDTO).toList();
        List<CardDTO> lowerRow = board.getLowerRow().stream().map(this::toCardDTO).toList();
        List<OfferTileDTO> offerTrack = board.getOfferTrack().stream().map(this::toOfferTileDTO).toList();
        List<TurnSlotDTO> turnOrder = board.getTurnOrderTile().getSlots().stream()
                .map(this::toTurnSlotDTO)
                .sorted(Comparator.comparingInt(TurnSlotDTO::positionIndex))
                .toList();

        return new GameStateDTO(
                game.getCurrentRound(),
                String.valueOf(game.getCurrentEra()),
                game.getCurrentPhaseName(),
                activePlayer == null ? null : activePlayer.getNickname(),
                players,
                upperRow,
                lowerRow,
                offerTrack,
                turnOrder
        );
    }

    public WinnerDTO toWinnerDTO(Player winner) {
        return new WinnerDTO(
                winner.getNickname(),
                Optional.ofNullable(winner.getTotem()).map(Totem::getColor).orElse(null),
                resolveFinalPP(winner),
                winner.getFood()
        );
    }

    private PlayerDTO toPlayerDTO(Player player, Player activePlayer) {
        Tribe tribe = player.getTribe();
        return new PlayerDTO(
                player.getNickname(),
                Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                player.getPP(),
                player.getFood(),
                resolveFinalPP(player),
                player == activePlayer,
                tribe.getBuilderDiscount(),
                tribe.countCharacters(CharacterType.COLLECTOR) * 3,
                tribe.getMembers().stream().map(this::toCardDTO).toList(),
                tribe.getBuildings().stream().map(this::toCardDTO).toList()
        );
    }

    private CardDTO toCardDTO(Card card) {
        Integer foodCost = card.getFoodCost() > 0 ? card.getFoodCost() : null;
        return new CardDTO(
                card.getId(),
                card.cardCategory(),
                card.cardDetailType(),
                String.valueOf(card.getEra()),
                card.getMinPlayers(),
                foodCost,
                card.getNumStars(),
                card.describe()
        );
    }

    private OfferTileDTO toOfferTileDTO(OfferTile tile) {
        return new OfferTileDTO(
                tile.getLetter(),
                tile.getUpperCardsToTake(),
                tile.getLowerCardsToTake(),
                tile.getFoodReward(),
                Optional.ofNullable(tile.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    private TurnSlotDTO toTurnSlotDTO(Slot slot) {
        return new TurnSlotDTO(
                slot.getPositionIndex(),
                slot.getFoodBonus(),
                slot.isLastSpace(),
                Optional.ofNullable(slot.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    /** Final PP, falling back to current PP if the calculation throws. */
    private int resolveFinalPP(Player player) {
        try {
            return player.finalPP();
        } catch (Exception ignored) {
            return player.getPP();
        }
    }
}
