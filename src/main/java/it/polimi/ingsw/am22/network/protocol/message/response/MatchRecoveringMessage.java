package it.polimi.ingsw.am22.network.protocol.message.response;

import it.polimi.ingsw.am22.network.protocol.message.ServerMessage;
import it.polimi.ingsw.am22.network.protocol.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.protocol.dto.GameStateDTO;

import java.util.List;

/**
 * Broadcast while a crash-recovered match is waiting for its players to
 * reconnect. The match stays paused — no moves are accepted — until every
 * original player is back; this message carries the current (frozen) board
 * plus how many players have returned and which are still missing.
 *
 * @param gameState        current board snapshot
 * @param reconnectedCount players that have reconnected so far
 * @param totalPlayers     players the match needs before it can resume
 * @param missingNicknames nicknames of players not yet reconnected
 */
public record MatchRecoveringMessage(GameStateDTO gameState,
                                     int reconnectedCount,
                                     int totalPlayers,
                                     List<String> missingNicknames) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
