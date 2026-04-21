package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * DTO serializzabile con i dati del giocatore vincitore.
 *
 * Allegato all'{@code EndGameMessage} al termine della partita.
 *
 * @param nickname             nickname del vincitore
 * @param totemColor           colore del totem
 * @param finalPrestigePoints  punti prestigio finali
 * @param remainingFood        cibo rimanente a fine partita
 */
public record WinnerDTO(
        String nickname,
        String totemColor,
        int finalPrestigePoints,
        int remainingFood
) implements Serializable {
}
