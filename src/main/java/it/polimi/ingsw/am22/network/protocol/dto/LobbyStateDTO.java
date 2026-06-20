package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable lobby snapshot, broadcast on every lobby change.
 *
 * @param matchId         the match id
 * @param hostNickname    the host's nickname
 * @param expectedPlayers the expected player count
 * @param started         whether the match has started
 * @param players         the players currently in the lobby
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
