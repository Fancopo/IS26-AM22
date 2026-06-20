package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A row of the historical match leaderboard.
 *
 * @param nickname   the player's nickname
 * @param score      the final score of that match
 * @param endDate    when the match ended
 * @param numPlayers the number of players in the match
 */
public record LeaderboardEntryDTO(
        String nickname,
        int score,
        LocalDateTime endDate,
        int numPlayers
) implements Serializable {
}
