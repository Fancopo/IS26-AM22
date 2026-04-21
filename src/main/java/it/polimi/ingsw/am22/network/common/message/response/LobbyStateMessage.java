package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.dto.LobbyStateDTO;

/**
 * Messaggio broadcast con lo stato aggiornato della lobby.
 *
 * Inviato ad ogni ingresso/uscita dalla lobby o cambio di configurazione
 * (es. numero di giocatori attesi) prima dell'inizio della partita.
 *
 * @param lobbyState snapshot dello stato della lobby
 */
public record LobbyStateMessage(LobbyStateDTO lobbyState) implements ServerMessage {
}
