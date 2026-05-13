package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/** Serializable per-player snapshot. Compact constructor makes lists immutable and non-null. */
public record PlayerDTO(
        String nickname,
        String totemColor,
        int prestigePoints,
        int food,
        int projectedFinalPrestigePoints,
        boolean active,
        int builderDiscount,
        int gathererDiscount,
        List<CardDTO> tribeCharacters,
        List<CardDTO> buildings
) implements Serializable {
    public PlayerDTO {
        tribeCharacters = tribeCharacters == null ? List.of() : List.copyOf(tribeCharacters);
        buildings = buildings == null ? List.of() : List.copyOf(buildings);
    }
}
