package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * DTO serializzabile che rappresenta uno slot del tracciato dell'ordine di turno.
 *
 * @param positionIndex indice della posizione nello slot
 * @param foodBonus     bonus di cibo associato alla posizione
 * @param lastSpace     {@code true} se è l'ultimo spazio del tracciato
 * @param occupiedBy    nickname del giocatore che occupa lo slot ({@code null} se libero)
 */
public record TurnSlotDTO(
        int positionIndex,
        int foodBonus,
        boolean lastSpace,
        String occupiedBy
) implements Serializable {
}
