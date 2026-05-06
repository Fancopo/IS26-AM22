package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable snapshot of the game state.
 *
 * Contains everything the client needs to render the game
 * without sharing the concrete model classes.
 * The compact constructor guarantees lists are immutable and
 * never {@code null}, making client-side deserialization safe.
 *
 * @param currentRound  current round number
 * @param currentEra    current era (as string)
 * @param currentPhase  current game phase name
 * @param activePlayer  nickname of the active player ({@code null} if none)
 * @param players       list of players and their state
 * @param upperRow      cards in the upper row of the board
 * @param lowerRow      cards in the lower row of the board
 * @param offerTrack    available offer tiles
 * @param turnOrder     turn-track slots ordered by position
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
