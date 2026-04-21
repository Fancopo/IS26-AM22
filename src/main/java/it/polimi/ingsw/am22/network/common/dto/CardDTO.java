package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

/**
 * DTO serializzabile che rappresenta una carta di gioco.
 *
 * Contiene solo dati, senza logica: viene generato dal {@code ModelDtoMapper}
 * a partire dalle classi concrete del modello (personaggi, edifici, eventi).
 *
 * @param id          identificatore univoco della carta
 * @param category    categoria (es. CHARACTER, BUILDING, EVENT)
 * @param detailType  tipo specifico all'interno della categoria
 * @param era         era di appartenenza della carta
 * @param minPlayers  numero minimo di giocatori per cui è attiva
 * @param foodCost    costo in cibo (può essere {@code null} se non applicabile)
 */
public record CardDTO(
        String id,
        String category,
        String detailType,
        String era,
        int minPlayers,
        Integer foodCost
) implements Serializable {
}
