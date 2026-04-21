package it.polimi.ingsw.am22.network.common.message.response;

import it.polimi.ingsw.am22.network.common.message.ServerMessage;

/**
 * Messaggio broadcast inviato quando la partita viene chiusa in modo anomalo.
 *
 * Tipicamente conseguenza della disconnessione di un giocatore mentre la
 * partita è in corso: la partita non può proseguire e tutti i client
 * vengono avvisati con una motivazione testuale.
 *
 * @param reason motivazione della chiusura della partita
 */
public record MatchClosedMessage(String reason) implements ServerMessage {
}
