package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;

/**
 * Broadcast sent when a suspended (crash-recovered) match is torn down because
 * one of its players chose to leave instead of reconnecting.
 *
 * <p>Distinct from {@link MatchClosedMessage}: that one keeps players on the
 * same connection at the matches list, whereas a player abandoning a recovery
 * ends the match entirely and sends everyone back to the very first scene
 * (transport choice), so each client starts a brand-new session from scratch.
 *
 * @param reason human-readable explanation shown to the other players
 */
public record MatchAbandonedMessage(String reason) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }

    // Not terminal: the server keeps the channel open after the broadcast. The
    // client decides to close its own session and return to the start screen,
    // mirroring the way MatchClosedMessage leaves channel lifecycle to the view.
}
