package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Conferma inviata al client dopo aver creato o essere entrato in una partita.
 *
 * Il client deve memorizzare {@code matchId} e allegarlo a tutte le richieste successive.
 */
public record MatchJoinedMessage(String matchId, String nickname) implements ServerMessage {
}
