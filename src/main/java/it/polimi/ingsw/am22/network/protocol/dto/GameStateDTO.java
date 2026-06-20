package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable snapshot of the game state. The compact constructor makes all
 * lists immutable and never {@code null}.
 *
 * @param currentRound the current round number
 * @param currentEra   the current Era
 * @param currentPhase the name of the current phase
 * @param activePlayer the active player's nickname, or {@code null}
 * @param players      per-player snapshots
 * @param upperRow     the upper row of cards on offer
 * @param lowerRow     the lower row of cards on offer
 * @param offerTrack   the offer-track tiles
 * @param turnOrder    the turn-order slots
 */
public record GameStateDTO(
        int currentRound,
        String currentEra,
        String currentPhase,
        String activePlayer,
        List<PlayerDTO> players,
        List<CardDTO> upperRow,
        List<CardDTO> lowerRow,
        List<OfferTileDTO> offerTrack,
        List<TurnSlotDTO> turnOrder
) implements Serializable {
    public GameStateDTO {
        players = players == null ? List.of() : List.copyOf(players);
        upperRow = upperRow == null ? List.of() : List.copyOf(upperRow);
        lowerRow = lowerRow == null ? List.of() : List.copyOf(lowerRow);
        offerTrack = offerTrack == null ? List.of() : List.copyOf(offerTrack);
        turnOrder = turnOrder == null ? List.of() : List.copyOf(turnOrder);
    }
}
