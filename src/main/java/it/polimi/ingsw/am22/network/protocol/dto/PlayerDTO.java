package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable per-player snapshot. The compact constructor makes the lists
 * immutable and non-null.
 *
 * @param nickname                     the player's nickname
 * @param totemColor                   the totem colour, or {@code null} if unassigned
 * @param prestigePoints               current prestige points
 * @param food                         current food
 * @param projectedFinalPrestigePoints projected final prestige points
 * @param active                       whether this is the active player
 * @param builderDiscount              the tribe's current Builder discount
 * @param gathererDiscount             the tribe's current Collector (gatherer) discount
 * @param tribeCharacters              the player's tribe characters
 * @param buildings                    the player's buildings
 */
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
