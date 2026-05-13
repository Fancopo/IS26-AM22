package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShamanicRitual extends Event implements EventEffect {

    public ShamanicRitual(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SHAMANIC_RITUAL, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int ppToAdd = switch (getEra()) {
            case I -> 5;
            case II -> 10;
            case III -> 15;
        };
        int ppToLose = switch (getEra()) {
            case I -> -3;
            case II -> -5;
            case III -> -7;
        };

        Map<Player, Integer> totalIcons = new HashMap<>();
        Map<Player, Boolean> preventLoss = new HashMap<>();
        Map<Player, Boolean> doubleWin = new HashMap<>();

        for (Player player : players) {
            int icons = 0;
            boolean noLoss = false;
            boolean doublePP = false;

            Tribe tribe = player.getTribe();
            if (tribe != null) {
                for (TribeCharacter character : tribe.getMembers()) {
                    icons += character.getNumStars();
                }
                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                    icons += effect.getExtraShamanIcons();
                    if (effect.preventsShamanPPLoss()) noLoss = true;
                    if (effect.doublesShamanWinPP()) doublePP = true;
                }
            }

            totalIcons.put(player, icons);
            preventLoss.put(player, noLoss);
            doubleWin.put(player, doublePP);
        }

        if (totalIcons.isEmpty()) return;

        int maxIcons = Collections.max(totalIcons.values());
        int minIcons = Collections.min(totalIcons.values());

        for (Player player : players) {
            int icons = totalIcons.get(player);
            if (icons == maxIcons) {
                player.addPP(doubleWin.get(player) ? ppToAdd * 2 : ppToAdd);
            }
            if (icons == minIcons && !preventLoss.get(player)) {
                player.addPP(ppToLose);
            }
        }
    }
}
