package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

public record TurnSlotDTO(
        int positionIndex,
        int foodBonus,
        boolean lastSpace,
        String occupiedBy
) implements Serializable {
}
