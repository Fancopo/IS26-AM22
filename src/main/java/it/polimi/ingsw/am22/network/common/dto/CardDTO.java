package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

public record CardDTO(
        String id,
        String category,
        String detailType,
        String era,
        int minPlayers,
        Integer foodCost
) implements Serializable {
}
