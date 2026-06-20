package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;

/**
 * Summary of an open match in the server-side registry, sent to the client when
 * it requests the list of available matches.
 *
 * @param matchId         the match id
 * @param hostNickname    the host's nickname
 * @param expectedPlayers the expected player count
 * @param currentPlayers  the number of players currently joined
 * @param started         whether the match has already started
 * @param recovering      true when the match survived a server crash and is
 *                        waiting for its players to reconnect
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
