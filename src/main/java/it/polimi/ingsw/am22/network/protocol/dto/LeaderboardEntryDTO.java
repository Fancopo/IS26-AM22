package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Riga della classifica storica delle partite.
 *
 * @param nickname     nickname del giocatore
 * @param score        punteggio finale di quella partita
 * @param endDate      data/ora in cui la partita si e' conclusa
 * @param numPlayers   numero di giocatori della partita
 */
public record LeaderboardEntryDTO(
        String nickname,
        int score,
        LocalDateTime endDate,
        int numPlayers
) implements Serializable {
}
