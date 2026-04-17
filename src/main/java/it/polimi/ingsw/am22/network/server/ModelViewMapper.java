package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.Event;
import it.polimi.ingsw.am22.network.common.view.*;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ModelViewMapper {

    public LobbyStateView toLobbyState(GameController gameController) {
        List<LobbyPlayerView> players = gameController.getLobbyPlayers().stream()
                .map(player -> new LobbyPlayerView(
                        player.getNickname(),
                        Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                        player.getNickname().equals(gameController.getHostNickname())
                ))
                .toList();

        return new LobbyStateView(
                gameController.getHostNickname(),
                gameController.getExpectedPlayers(),
                gameController.hasStarted(),
                players
        );
    }

    public GameStateView toGameState(Game game) {
        Board board = game.getBoard();
        Player activePlayer = game.getActivePlayer();

        List<PlayerView> players = game.getPlayers().stream()
                .map(player -> toPlayerView(player, activePlayer))
                .toList();

        List<CardView> upperRow = board.getUpperRow().stream().map(this::toCardView).toList();
        List<CardView> lowerRow = board.getLowerRow().stream().map(this::toCardView).toList();
        List<OfferTileView> offerTrack = board.getOfferTrack().stream().map(this::toOfferTileView).toList();
        List<TurnSlotView> turnOrder = board.getTurnOrderTile().getSlots().stream()
                .map(this::toTurnSlotView)
                .sorted(Comparator.comparingInt(TurnSlotView::positionIndex))
                .toList();

        return new GameStateView(
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

    public WinnerView toWinnerView(Player winner) {
        return new WinnerView(
                winner.getNickname(),
                Optional.ofNullable(winner.getTotem()).map(Totem::getColor).orElse(null),
                resolveFinalPP(winner),
                winner.getFood()
        );
    }

    private PlayerView toPlayerView(Player player, Player activePlayer) {
        Tribe tribe = player.getTribe();

        return new PlayerView(
                player.getNickname(),
                Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                player.getPP(),
                player.getFood(),
                resolveFinalPP(player),
                player == activePlayer,
                tribe.getMembers().stream().map(this::toCardView).toList(),
                tribe.getBuildings().stream().map(this::toCardView).toList()
        );
    }

    private CardView toCardView(Card card) {
        return new CardView(
                card.getId(),
                categoryOf(card),
                detailTypeOf(card),
                String.valueOf(card.getEra()),
                card.getMinPlayers(),
                foodCostOf(card)
        );
    }

    private OfferTileView toOfferTileView(OfferTile tile) {
        return new OfferTileView(
                tile.getLetter(),
                tile.getUpperCardsToTake(),
                tile.getLowerCardsToTake(),
                tile.getFoodReward(),
                Optional.ofNullable(tile.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    private TurnSlotView toTurnSlotView(Slot slot) {
        return new TurnSlotView(
                slot.getPositionIndex(),
                slot.getFoodBonus(),
                slot.isLastSpace(),
                Optional.ofNullable(slot.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    private int resolveFinalPP(Player player) {
        try {
            return player.finalPP();
        } catch (Exception ignored) {
        }
        return player.getPP();
    }

    private String categoryOf(Card card) {
        if (card instanceof TribeCharacter) return "CHARACTER";
        if (card instanceof Building) return "BUILDING";
        if (card instanceof Event) return "EVENT";
        return card.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    private String detailTypeOf(Card card) {
        if (card instanceof Building) return "BUILDING";
        if (card instanceof TribeCharacter tc) return String.valueOf(tc.getCharacterType());
        if (card instanceof Event ev) return String.valueOf(ev.getEventType());
        return card.getClass().getSimpleName().toUpperCase(Locale.ROOT);
    }

    private Integer foodCostOf(Card card) {
        if (card instanceof Building building) return building.getFoodPrice();
        try {
            Method method = card.getClass().getMethod("getFoodCost");
            Object value = method.invoke(card);
            if (value instanceof Number number) return number.intValue();
        } catch (Exception ignored) {
        }
        return null;
    }
}
