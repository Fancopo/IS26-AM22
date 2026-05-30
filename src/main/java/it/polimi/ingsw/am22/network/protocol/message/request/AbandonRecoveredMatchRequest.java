package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request a reconnecting player sends when they choose "Leave this match" on
 * the reconnect screen instead of resuming a match that survived a server crash.
 *
 * <p>Unlike a normal disconnect, the sender is not yet bound to the match: it
 * reconnected for the sole purpose of tearing the suspended match down. The
 * server discards the match's snapshot and tells every player that had already
 * reconnected that the match is over, so nobody is left waiting forever for a
 * player that will never return.
 *
 * @param matchId id of the suspended match to abandon
 */
public record AbandonRecoveredMatchRequest(String matchId) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
