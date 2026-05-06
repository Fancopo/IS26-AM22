package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;

import java.util.List;

/**
 * Server response listing matches that are open and not yet started.
 */
public record MatchesListMessage(List<MatchInfoDTO> matches) implements ServerMessage {
    public MatchesListMessage {
        matches = matches == null ? List.of() : List.copyOf(matches);
    }

    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
