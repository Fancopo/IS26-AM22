package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;

/** Serializable card DTO. Data-only; built by {@code ModelDtoMapper}. */
public record CardDTO(
        String id,
        String category,
        String detailType,
        String era,
        int minPlayers,
        Integer foodCost,
        int numStars
) implements Serializable {
}
