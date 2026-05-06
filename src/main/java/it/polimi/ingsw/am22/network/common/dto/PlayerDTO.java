package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable snapshot of a player in the match.
 *
 * The compact constructor guarantees lists are immutable and never {@code null}.
 *
 * @param nickname                       player nickname
 * @param totemColor                     totem color
 * @param prestigePoints                 current prestige points
 * @param food                           available food
 * @param projectedFinalPrestigePoints   projected final prestige points
 * @param active                         {@code true} if this is the active player
 * @param tribeCharacters                tribe character cards
 * @param buildings                      owned buildings
 */
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
