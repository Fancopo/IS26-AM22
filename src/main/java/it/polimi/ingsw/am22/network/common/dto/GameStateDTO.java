package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Snapshot serializzabile dello stato di gioco.
 *
 * Contiene tutto ciò che serve al client per rappresentare la partita
 * senza dover condividere le classi concrete del modello.
 * Il costruttore compatto garantisce che le liste siano immutabili e
 * mai {@code null}, rendendo sicura la deserializzazione lato client.
 *
 * @param currentRound  numero del round corrente
 * @param currentEra    era corrente (rappresentata come stringa)
 * @param currentPhase  nome della fase di gioco corrente
 * @param activePlayer  nickname del giocatore di turno ({@code null} se nessuno)
 * @param players       lista dei giocatori con il loro stato
 * @param upperRow      carte della riga superiore sulla board
 * @param lowerRow      carte della riga inferiore sulla board
 * @param offerTrack    tessere offerta disponibili
 * @param turnOrder     slot del tracciato turno ordinati per posizione
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
