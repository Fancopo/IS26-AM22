package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;

public record LobbyPlayerView(String nickname, String totemColor, boolean host) implements Serializable {
}
