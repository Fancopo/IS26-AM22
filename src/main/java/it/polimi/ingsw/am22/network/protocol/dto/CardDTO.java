package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;

/**
 * Serializable card DTO. Data-only; built by {@code ModelDtoMapper}.
 *
 * @param id          the card id
 * @param category    the macro-category ("CHARACTER", "BUILDING", "EVENT")
 * @param detailType  the specific type within the category
 * @param era         the card's Era
 * @param minPlayers  the minimum player count for the card
 * @param foodCost    the food cost, or {@code null} if the card is free
 * @param numStars    the number of Shaman stars on the card
 * @param description a human-readable description of the card's effect
 */
public record CardDTO(
        String id,
        String category,
        String detailType,
        String era,
        int minPlayers,
        Integer foodCost,
        int numStars,
        String description
) implements Serializable {
}
