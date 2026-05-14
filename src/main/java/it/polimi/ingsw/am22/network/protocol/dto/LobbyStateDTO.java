package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.util.List;

/** Serializable lobby snapshot, broadcast on every lobby change. */
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
