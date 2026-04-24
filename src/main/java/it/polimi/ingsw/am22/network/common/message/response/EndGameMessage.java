package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.WinnerDTO;

/**
 * Messaggio broadcast inviato quando la partita termina.
 *
 * Contiene i dati del vincitore e lo snapshot finale dello stato di gioco.
 *
 * @param winner         dati del giocatore vincitore
 * @param finalGameState stato finale della partita
 */
public record EndGameMessage(WinnerDTO winner, GameStateDTO finalGameState) implements ServerMessage {
    @Override
    public void accept(ServerMessageVisitor visitor) { visitor.visit(this); }
}
