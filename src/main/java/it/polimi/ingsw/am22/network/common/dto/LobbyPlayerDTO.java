package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * Serializable DTO representing a player in the lobby.
 *
 * @param nickname   player nickname
 * @param totemColor chosen totem color ({@code null} if not yet assigned)
 * @param host       {@code true} if the player is the lobby host
 */
public record LobbyPlayerDTO(String nickname, String totemColor, boolean host) implements Serializable {
}
