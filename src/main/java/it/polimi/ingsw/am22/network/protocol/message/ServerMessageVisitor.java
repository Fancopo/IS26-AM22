package it.polimi.ingsw.am22.network.protocol.message;

import it.polimi.ingsw.am22.network.protocol.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchAbandonedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchRecoveringMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.MatchesListMessage;
import it.polimi.ingsw.am22.network.protocol.message.response.PingMessage;

/**
 * Visitor for server-to-client messages. Dispatch goes through {@link ServerMessage#accept}.
 * All methods default to no-op so implementers only override the messages they care about.
 */
public interface ServerMessageVisitor {
    default void visit(MatchesListMessage message) {}
    default void visit(MatchJoinedMessage message) {}
    default void visit(LobbyStateMessage message) {}
    default void visit(GameStartedMessage message) {}
    default void visit(GameStateMessage message) {}
    default void visit(EndGameMessage message) {}
    default void visit(MatchClosedMessage message) {}
    default void visit(MatchAbandonedMessage message) {}
    default void visit(MatchRecoveringMessage message) {}
    default void visit(ErrorMessage message) {}
    /** Transport-only liveness probe. Filtered by the read loop, never reaches the view. */
    default void visit(PingMessage message) {}
}
