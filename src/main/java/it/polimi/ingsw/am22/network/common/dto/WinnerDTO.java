package it.polimi.ingsw.am22.network.common.dto;

import java.io.Serializable;

public record WinnerDTO(
        String nickname,
        String totemColor,
        int finalPrestigePoints,
        int remainingFood
) implements Serializable {
}
