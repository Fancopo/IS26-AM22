package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.dto.TotemSelectionStateDTO;

/**
 * Broadcast message carrying the current state of the pre-game totem selection
 * phase. Sent when the lobby fills up (selection begins) and after each pick,
 * until everyone has chosen and a {@link GameStartedMessage} is sent instead.
 *
 * @param selectionState totem-selection snapshot
 */
public record TotemSelectionMessage(TotemSelectionStateDTO selectionState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
