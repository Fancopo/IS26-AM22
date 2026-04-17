package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

public record LobbyStateDTO(
        String hostNickname,
        int expectedPlayers,
        boolean started,
        List<LobbyPlayerDTO> players
) implements Serializable {
    public LobbyStateDTO {
        players = players == null ? List.of() : List.copyOf(players);
    }
}
