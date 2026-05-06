package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable snapshot of the lobby state.
 *
 * Broadcast on every lobby change (joins, leaves, configuration).
 * The compact constructor guarantees the player list is
 * immutable and never {@code null}.
 *
 * @param hostNickname    host's nickname
 * @param expectedPlayers number of players required to start the match
 * @param started         {@code true} if the match has started
 * @param players         players currently in the lobby
 */
public record LobbyStateDTO(
        String matchId,
        String hostNickname,
        int expectedPlayers,
        boolean started,
        List<LobbyPlayerDTO> players
) implements Serializable {
    public LobbyStateDTO {
        players = players == null ? List.of() : List.copyOf(players);
    }
}
