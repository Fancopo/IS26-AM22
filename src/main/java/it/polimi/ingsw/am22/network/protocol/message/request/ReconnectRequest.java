package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

/**
 * Request to rejoin a match that survived a server crash.
 *
 * <p>After the server restarts it resurrects in-progress matches from disk;
 * a player reconnects by sending this request with the {@code matchId} their
 * client still holds and the {@code nickname} they used before. The server
 * resumes the match for them only if the nickname matches a player of that
 * suspended match.
 *
 * @param matchId  id of the suspended match to rejoin
 * @param nickname nickname the player used in the original match
 */
public record ReconnectRequest(String matchId, String nickname) implements ClientRequest {
    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
