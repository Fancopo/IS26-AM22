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
import it.polimi.ingsw.am22.network.protocol.message.response.TotemSelectionMessage;

/**
 * Visitor for server-to-client messages. Dispatch goes through {@link ServerMessage#accept}.
 * All methods default to no-op so implementers only override the messages they care about.
 */
public interface ServerMessageVisitor {
    /** @param message the matches-list message */
    default void visit(MatchesListMessage message) {}

    /** @param message the match-joined message */
    default void visit(MatchJoinedMessage message) {}

    /** @param message the lobby-state message */
    default void visit(LobbyStateMessage message) {}

    /** @param message the totem-selection message */
    default void visit(TotemSelectionMessage message) {}

    /** @param message the game-started message */
    default void visit(GameStartedMessage message) {}

    /** @param message the game-state message */
    default void visit(GameStateMessage message) {}

    /** @param message the end-game message */
    default void visit(EndGameMessage message) {}

    /** @param message the match-closed message */
    default void visit(MatchClosedMessage message) {}

    /** @param message the match-abandoned message */
    default void visit(MatchAbandonedMessage message) {}

    /** @param message the match-recovering message */
    default void visit(MatchRecoveringMessage message) {}

    /** @param message the error message */
    default void visit(ErrorMessage message) {}

    /**
     * Transport-only liveness probe. Filtered by the read loop, never reaches the view.
     *
     * @param message the ping message
     */
    default void visit(PingMessage message) {}
}
