package it.polimi.ingsw.am22.network.protocol.message.request;

import it.polimi.ingsw.am22.network.protocol.message.ClientRequest;
import it.polimi.ingsw.am22.network.protocol.message.ClientRequestVisitor;

import java.util.List;

/**
 * Request to pick one or more cards from the board.
 *
 * The compact constructor makes the list immutable and replaces
 * {@code null} with an empty list, removing receiver-side checks.
 *
 * @param matchId         match identifier
 * @param playerNickname  nickname of the acting player
 * @param selectedCardIds (immutable) list of selected card ids
 */
public record PickCardsRequest(String matchId, String playerNickname, List<String> selectedCardIds) implements ClientRequest {
    public PickCardsRequest {
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
    }

    @Override
    public void accept(ClientRequestVisitor visitor) { visitor.visit(this); }
}
