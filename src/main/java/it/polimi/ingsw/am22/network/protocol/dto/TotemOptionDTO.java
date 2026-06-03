package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;

/**
 * Serializable DTO for a single totem color in the selection phase.
 *
 * @param color         totem color (e.g. "Red", "Blue", ...)
 * @param ownerNickname nickname of the player who already chose this color,
 *                      or {@code null} if the color is still free
 */
public record TotemOptionDTO(String color, String ownerNickname) implements Serializable {
}
