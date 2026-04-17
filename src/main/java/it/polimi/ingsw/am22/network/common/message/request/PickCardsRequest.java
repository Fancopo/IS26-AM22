package it.polimi.ingsw.am22.network.common.message.request;

import it.polimi.ingsw.am22.network.common.message.ClientRequest;

import java.util.List;

public record PickCardsRequest(String playerNickname, List<String> selectedCardIds) implements ClientRequest {
    public PickCardsRequest {
        selectedCardIds = selectedCardIds == null ? List.of() : List.copyOf(selectedCardIds);
    }
}
