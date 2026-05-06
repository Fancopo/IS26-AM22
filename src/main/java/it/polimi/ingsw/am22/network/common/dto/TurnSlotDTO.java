package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * Serializable DTO representing a slot on the turn-order track.
 *
 * @param positionIndex slot position index
 * @param foodBonus     food bonus tied to the position
 * @param lastSpace     {@code true} if this is the last space of the track
 * @param occupiedBy    nickname of the player occupying the slot ({@code null} if free)
 */
public record TurnSlotDTO(
        int positionIndex,
        int foodBonus,
        boolean lastSpace,
        String occupiedBy
) implements Serializable {
}
