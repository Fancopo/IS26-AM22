package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * Serializable DTO with the winning player's data.
 *
 * Attached to {@code EndGameMessage} at the end of a match.
 *
 * @param nickname             winner's nickname
 * @param totemColor           totem color
 * @param finalPrestigePoints  final prestige points
 * @param remainingFood        food left at end of match
 */
public record WinnerDTO(
        String nickname,
        String totemColor,
        int finalPrestigePoints,
        int remainingFood
) implements Serializable {
}
