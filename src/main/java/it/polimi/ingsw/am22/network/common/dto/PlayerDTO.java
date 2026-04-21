package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Snapshot serializzabile di un giocatore in partita.
 *
 * Il costruttore compatto garantisce che le liste siano immutabili e mai {@code null}.
 *
 * @param nickname                       nickname del giocatore
 * @param totemColor                     colore del totem
 * @param prestigePoints                 punti prestigio attuali
 * @param food                           cibo disponibile
 * @param projectedFinalPrestigePoints   punti prestigio finali previsti
 * @param active                         {@code true} se è il giocatore di turno
 * @param tribeCharacters                carte personaggio della tribù
 * @param buildings                      edifici posseduti
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
