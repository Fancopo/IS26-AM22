package it.polimi.ingsw.am22.network.protocol.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Serializable snapshot of the pre-game totem selection phase, broadcast when
 * the lobby fills up and after every pick.
 *
 * @param options        all selectable totem colors, each with its current
 *                       owner ({@code null} if still free)
 * @param currentChooser nickname of the player whose turn it is to choose,
 *                       or {@code null} when no one is choosing
 */
public record TotemSelectionStateDTO(
        List<TotemOptionDTO> options,
        String currentChooser
) implements Serializable {
    public TotemSelectionStateDTO {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
