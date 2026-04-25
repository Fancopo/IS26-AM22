package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

/**
 * Richiesta della lista delle partite aperte (non ancora iniziate).
 *
 * Il server risponde con un {@code MatchesListMessage}.
 */
public record ListMatchesRequest() implements ClientRequest {
}
