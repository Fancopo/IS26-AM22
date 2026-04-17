package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

public record LobbyPlayerDTO(String nickname, String totemColor, boolean host) implements Serializable {
}
