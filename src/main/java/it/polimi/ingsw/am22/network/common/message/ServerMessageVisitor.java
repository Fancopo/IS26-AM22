package it.polimi.ingsw.am22.network.common.message;

import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

/**
 * Visitor per i messaggi inviati dal server al client.
 *
 * Ogni implementazione definisce il comportamento per ciascun tipo di
 * {@link ServerMessage}: la dispatch avviene tramite {@link ServerMessage#accept}
 * senza usare instanceof.
 */
public interface ServerMessageVisitor {
    void visit(LobbyStateMessage message);
    void visit(GameStartedMessage message);
    void visit(GameStateMessage message);
    void visit(EndGameMessage message);
    void visit(MatchClosedMessage message);
    void visit(ErrorMessage message);
    void visit(InfoMessage message);
}
