package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;

/**
 * Summary of an open match in the server-side registry.
 *
 * Sent to the client when it requests the list of available matches.
 */
/**
 * @param recovering true when the match survived a server crash and is
 *                   waiting for its players to reconnect
 */
public record MatchInfoDTO(
        String matchId,
        String hostNickname,
        int expectedPlayers,
        int currentPlayers,
        boolean started,
        boolean recovering
) implements Serializable {
}
