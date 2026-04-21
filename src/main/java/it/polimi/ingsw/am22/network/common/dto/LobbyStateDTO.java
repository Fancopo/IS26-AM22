package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Snapshot serializzabile dello stato della lobby.
 *
 * Trasmesso ad ogni variazione della lobby (ingressi, uscite, configurazione).
 * Il costruttore compatto garantisce che la lista dei giocatori sia
 * immutabile e mai {@code null}.
 *
 * @param hostNickname    nickname dell'host
 * @param expectedPlayers numero di giocatori attesi per avviare la partita
 * @param started         {@code true} se la partita è iniziata
 * @param players         lista dei giocatori attualmente in lobby
 */
public record LobbyStateDTO(
        String hostNickname,
        int expectedPlayers,
        boolean started,
        List<LobbyPlayerDTO> players
) implements Serializable {
    public LobbyStateDTO {
        players = players == null ? List.of() : List.copyOf(players);
    }
}
