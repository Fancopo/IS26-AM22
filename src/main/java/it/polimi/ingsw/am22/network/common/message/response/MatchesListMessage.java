package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;

import java.util.List;

/**
 * Risposta del server con la lista delle partite aperte e non ancora iniziate.
 */
public record MatchesListMessage(List<MatchInfoDTO> matches) implements ServerMessage {
    public MatchesListMessage {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }
}
