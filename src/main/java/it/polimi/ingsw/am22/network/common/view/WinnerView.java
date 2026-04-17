package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;

public record WinnerView(
        String nickname,
        String totemColor,
        int finalPrestigePoints,
        int remainingFood
) implements Serializable {
}
