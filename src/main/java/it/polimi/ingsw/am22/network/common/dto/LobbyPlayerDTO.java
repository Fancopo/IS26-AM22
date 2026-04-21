package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * DTO serializzabile che rappresenta un giocatore in lobby.
 *
 * @param nickname   nickname del giocatore
 * @param totemColor colore del totem scelto ({@code null} se non ancora assegnato)
 * @param host       {@code true} se il giocatore è l'host della lobby
 */
public record LobbyPlayerDTO(String nickname, String totemColor, boolean host) implements Serializable {
}
