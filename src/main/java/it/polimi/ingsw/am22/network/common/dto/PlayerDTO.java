package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

public record PlayerDTO(
        String nickname,
        String totemColor,
        int prestigePoints,
        int food,
        int projectedFinalPrestigePoints,
        boolean active,
        List<CardDTO> tribeCharacters,
        List<CardDTO> buildings
) implements Serializable {
    public PlayerDTO {
        tribeCharacters = tribeCharacters == null ? List.of() : List.copyOf(tribeCharacters);
        buildings = buildings == null ? List.of() : List.copyOf(buildings);
    }
}
