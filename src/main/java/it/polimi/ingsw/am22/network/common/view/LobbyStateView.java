package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;
import java.util.List;

public record LobbyStateView(
        String hostNickname,
        int expectedPlayers,
        boolean started,
        List<LobbyPlayerView> players
) implements Serializable {
    public LobbyStateView {
        players = players == null ? List.of() : List.copyOf(players);
    }
}
