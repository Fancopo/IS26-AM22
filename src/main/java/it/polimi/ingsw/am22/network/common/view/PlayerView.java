package it.polimi.ingsw.am22.network.common.view;

import java.io.Serializable;
import java.util.List;

public record PlayerView(
        String nickname,
        String totemColor,
        int prestigePoints,
        int food,
        int projectedFinalPrestigePoints,
        boolean active,
        List<CardView> tribeCharacters,
        List<CardView> buildings
) implements Serializable {
    public PlayerView {
        tribeCharacters = tribeCharacters == null ? List.of() : List.copyOf(tribeCharacters);
        buildings = buildings == null ? List.of() : List.copyOf(buildings);
    }
}
