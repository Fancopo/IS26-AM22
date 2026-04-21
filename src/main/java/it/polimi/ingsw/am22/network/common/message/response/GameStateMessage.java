package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;

/**
 * Messaggio broadcast con lo stato corrente della partita.
 *
 * Inviato ad ogni cambiamento significativo (azione del giocatore, cambio turno, ecc.).
 *
 * @param gameState snapshot aggiornato dello stato di gioco
 */
public record GameStateMessage(GameStateDTO gameState) implements ServerMessage {
}
