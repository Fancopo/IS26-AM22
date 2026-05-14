package it.polimi.ingsw.am22.network.common.message;

import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage;

/** Visitor for server-to-client messages. Dispatch goes through {@link ServerMessage#accept}. */
public interface ServerMessageVisitor {
    void visit(MatchesListMessage message);
    void visit(MatchJoinedMessage message);
    void visit(LobbyStateMessage message);
    void visit(GameStartedMessage message);
    void visit(GameStateMessage message);
    void visit(EndGameMessage message);
    void visit(MatchClosedMessage message);
    void visit(ErrorMessage message);
}
