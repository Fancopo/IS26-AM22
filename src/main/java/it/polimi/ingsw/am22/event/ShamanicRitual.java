package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ShamanicRitual implements EventEffect {

    private Era era;

    public ShamanicRitual(Era era) {
        this.era = era;
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPtoAdd = 0;
        int PPtoLose = 0;

        if (era == Era.I) {
            PPtoAdd = 5;
            PPtoLose = -3;
        } else if (era == Era.II) {
            PPtoAdd = 10;
            PPtoLose = -5;
        } else if (era == Era.III) {
            PPtoAdd = 15;
            PPtoLose = -7;
        }

        Map<Player, Integer> totalIconsPerPlayer = new HashMap<>();
        Map<Player, Boolean> preventLoss = new HashMap<>();
        Map<Player, Boolean> doubleWin = new HashMap<>();

        for (Player player : players) {
            int baseIcons = 0;
            int extraIcons = 0;
            boolean noLoss = false;
            boolean doublePP = false;

            Tribe tribe = player.getTribe();
            if (tribe != null) {
                for (TribeCharacter character : tribe.getMembers()) {
                    if (character.getCharacterType() == CharacterType.SHAMAN) {
                        baseIcons += character.getNumStars();
                    }
                }

                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                    extraIcons += effect.getExtraShamanIcons();
                    if (effect.preventsShamanPPLoss()) noLoss = true;
                    if (effect.doublesShamanWinPP()) doublePP = true;
                }
            }

            totalIconsPerPlayer.put(player, baseIcons + extraIcons);
            preventLoss.put(player, noLoss);
            doubleWin.put(player, doublePP);
        }

        if (totalIconsPerPlayer.isEmpty()) return;

        int maxIcons = Collections.max(totalIconsPerPlayer.values());
        int minIcons = Collections.min(totalIconsPerPlayer.values());

        for (Player player : players) {
            int icons = totalIconsPerPlayer.get(player);

            if (icons == maxIcons) {
                int earnedPP = doubleWin.get(player) ? PPtoAdd * 2 : PPtoAdd;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " vince il rito e ottiene " + earnedPP + " PP!");
            }

            if (icons == minIcons) {
                if (preventLoss.get(player)) {
                    System.out.println(player.getNickname() + " ha meno icone, ma l'Edificio lo protegge dalla perdita di PP!");
                } else {
                    player.addPP(PPtoLose);
                    System.out.println(player.getNickname() + " perde il rito e subisce " + PPtoLose + " PP.");
                }
            }
        }
    }
}