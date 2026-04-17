package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;

public record TurnSlotView(
        int positionIndex,
        int foodBonus,
        boolean lastSpace,
        String occupiedBy
) implements Serializable {
}
