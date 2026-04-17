package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;

public record CardView(
        String id,
        String category,
        String detailType,
        String era,
        int minPlayers,
        Integer foodCost
) implements Serializable {
}
