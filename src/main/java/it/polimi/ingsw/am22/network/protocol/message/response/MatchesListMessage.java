package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.dto.MatchInfoDTO;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

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
