package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * Sintesi di una partita aperta nel registry lato server.
 *
 * Viene inviata al client quando chiede la lista delle partite disponibili.
 */
public record MatchInfoDTO(
        String matchId,
        String hostNickname,
        int expectedPlayers,
        int currentPlayers,
        boolean started
) implements Serializable {
}
