package it.polimi.ingsw.am22.network.server;

import it.polimi.ingsw.am22.controller.GameController;
import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.network.common.dto.*;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Converte le classi del model nei DTO serializzabili del package
 * {@code network.common.dto}.
 *
 * È l'unico punto in cui il layer di rete "legge" il model: tutto il resto
 * (handler, VirtualView, ecc.) lavora solo sui DTO, così il model non
 * viaggia mai in rete e non deve essere {@link java.io.Serializable}.
 */
public class ModelDtoMapper {

    /**
     * Produce lo snapshot della lobby a partire dal {@link GameController}.
     *
     * @param gameController controller da cui leggere i dati di lobby
     * @return DTO della lobby
     */
    public LobbyStateDTO toLobbyState(GameController gameController) {
        List<LobbyPlayerDTO> players = gameController.getLobbyPlayers().stream()
                .map(player -> new LobbyPlayerDTO(
                        player.getNickname(),
                        Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                        player.getNickname().equals(gameController.getHostNickname())
                ))
                .toList();

        return new LobbyStateDTO(
                gameController.getHostNickname(),
                gameController.getExpectedPlayers(),
                gameController.hasStarted(),
                players
        );
    }

    /**
     * Produce lo snapshot di gioco a partire dallo stato corrente del {@link Game}.
     *
     * @param game partita corrente
     * @return DTO con giocatori, board e ordine di turno
     */
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

    /**
     * Produce il DTO del giocatore vincitore.
     *
     * @param winner giocatore vincitore
     * @return DTO con nickname, colore, punti finali e cibo residuo
     */
    public WinnerDTO toWinnerDTO(Player winner) {
        return new WinnerDTO(
                winner.getNickname(),
                Optional.ofNullable(winner.getTotem()).map(Totem::getColor).orElse(null),
                resolveFinalPP(winner),
                winner.getFood()
        );
    }

    /** Costruisce un {@link PlayerDTO} segnalando se il giocatore è quello di turno. */
    private PlayerDTO toPlayerDTO(Player player, Player activePlayer) {
        Tribe tribe = player.getTribe();

        return new PlayerDTO(
                player.getNickname(),
                Optional.ofNullable(player.getTotem()).map(Totem::getColor).orElse(null),
                player.getPP(),
                player.getFood(),
                resolveFinalPP(player),
                player == activePlayer,
                tribe.getMembers().stream().map(this::toCardDTO).toList(),
                tribe.getBuildings().stream().map(this::toCardDTO).toList()
        );
    }

    /** Estrae categoria, tipo specifico ed era da una {@link Card}. */
    private CardDTO toCardDTO(Card card) {
        return new CardDTO(
                card.getId(),
                categoryOf(card),
                detailTypeOf(card),
                String.valueOf(card.getEra()),
                card.getMinPlayers(),
                foodCostOf(card)
        );
    }

    /** Mappa una tessera offerta; {@code occupiedBy} è il nickname di chi la occupa. */
    private OfferTileDTO toOfferTileDTO(OfferTile tile) {
        return new OfferTileDTO(
                tile.getLetter(),
                tile.getUpperCardsToTake(),
                tile.getLowerCardsToTake(),
                tile.getFoodReward(),
                Optional.ofNullable(tile.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    /** Mappa uno slot del tracciato turno; {@code occupiedBy} è il nickname di chi lo occupa. */
    private TurnSlotDTO toTurnSlotDTO(Slot slot) {
        return new TurnSlotDTO(
                slot.getPositionIndex(),
                slot.getFoodBonus(),
                slot.isLastSpace(),
                Optional.ofNullable(slot.getOccupiedBy()).map(Totem::getOwner).map(Player::getNickname).orElse(null)
        );
    }

    /** Risolve i punti prestigio finali; se il calcolo fallisce torna ai PP correnti. */
    private int resolveFinalPP(Player player) {
        try {
            return player.finalPP();
        } catch (Exception ignored) {
        }
        return player.getPP();
    }

    /** Macro-categoria della carta delegata alla carta stessa. */
    private String categoryOf(Card card) {
        return card.cardCategory();
    }

    /** Tipo specifico della carta delegato alla carta stessa. */
    private String detailTypeOf(Card card) {
        return card.cardDetailType();
    }

    /** Costo in cibo della carta; restituisce null se zero (carta senza costo). */
    private Integer foodCostOf(Card card) {
        int cost = card.getFoodCost();
        return cost > 0 ? cost : null;
    }
}
