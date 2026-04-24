package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;

/**
 * Messaggio broadcast inviato nel momento esatto in cui la partita inizia.
 *
 * Permette ai client di uscire dallo stato "lobby" e mostrare l'interfaccia di gioco.
 *
 * @param initialGameState snapshot iniziale dello stato di gioco
 */
public record GameStartedMessage(GameStateDTO initialGameState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
