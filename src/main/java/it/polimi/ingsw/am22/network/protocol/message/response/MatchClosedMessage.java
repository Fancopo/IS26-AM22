package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

/**
 * Broadcast message sent when the match is closed abnormally.
 *
 * Typically caused by a player disconnecting while the match
 * is running: the match cannot continue and all clients
 * are notified with a textual reason.
 *
 * @param reason reason the match was closed
 */
public record MatchClosedMessage(String reason) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }

    // Not terminal for the transport: the server keeps the channel open after
    // this message so clients can return to the matches list and list/create/
    // join again. A synthetic disconnect here would wrongly drop RMI clients
    // back to the connection screen instead of the matches screen.
}
