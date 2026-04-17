package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;
import java.util.List;

public record GameStateView(
        int currentRound,
        String currentEra,
        String currentPhase,
        String activePlayer,
        List<PlayerView> players,
        List<CardView> upperRow,
        List<CardView> lowerRow,
        List<OfferTileView> offerTrack,
        List<TurnSlotView> turnOrder
) implements Serializable {
    public GameStateView {
        players = players == null ? List.of() : List.copyOf(players);
        upperRow = upperRow == null ? List.of() : List.copyOf(upperRow);
        lowerRow = lowerRow == null ? List.of() : List.copyOf(lowerRow);
        offerTrack = offerTrack == null ? List.of() : List.copyOf(offerTrack);
        turnOrder = turnOrder == null ? List.of() : List.copyOf(turnOrder);
    }
}
