package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

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
