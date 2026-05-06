package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ShamanicRitual extends Event implements EventEffect {

    public ShamanicRitual(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SHAMANIC_RITUAL, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPtoAdd = 0;
        int PPtoLose = 0;

        if (this.getEra() == Era.I) {
            PPtoAdd = 5;
            PPtoLose = -3;
        } else if (this.getEra() == Era.II) {
            PPtoAdd = 10;
            PPtoLose = -5;
        } else if (this.getEra() == Era.III) {
            PPtoAdd = 15;
            PPtoLose = -7;
        }

        Map<Player, Integer> totalIconsPerPlayer = new HashMap<>();
        Map<Player, Boolean> preventLoss = new HashMap<>();
        Map<Player, Boolean> doubleWin = new HashMap<>();

        // 1. ICON COUNT AND BUILDING MODIFIERS
        for (Player player : players) {
            int baseIcons = 0;
            int extraIcons = 0;
            boolean noLoss = false;
            boolean doublePP = false;

            Tribe tribe = player.getTribe();
            if (tribe != null) {
                // Count base Shaman icons
                for (TribeCharacter character : tribe.getMembers()) {
                        baseIcons += character.getNumStars();
                }

                // Apply Building modifiers for the Shamanic Ritual
                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                        // Modifier: 3 extra icons
                        extraIcons += effect.getExtraShamanIcons();
                        // Modifier: do not lose Prestige Points
                        if (effect.preventsShamanPPLoss()) noLoss = true;
                        // Modifier: double the gained Prestige Points
                        if (effect.doublesShamanWinPP()) doublePP = true;
                    }
                }

            totalIconsPerPlayer.put(player, baseIcons + extraIcons);
            preventLoss.put(player, noLoss);
            doubleWin.put(player, doublePP);
        }


        if (totalIconsPerPlayer.isEmpty()) return;

        // 2. FIND THE MAX AND MIN
        int maxIcons = Collections.max(totalIconsPerPlayer.values());
        int minIcons = Collections.min(totalIconsPerPlayer.values());

        // 3. ASSIGN REWARDS AND PENALTIES
        for (Player player : players) {
            int icons = totalIconsPerPlayer.get(player);

            // Win: the player with the most icons in their tribe gains the listed PP.
            // In case of a tie, all tied players get the PP.
            if (icons == maxIcons) {
                int earnedPP = doubleWin.get(player) ? PPtoAdd * 2 : PPtoAdd;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " wins the ritual and gains " + earnedPP + " PP!");
            }

            // Lose: the player with the fewest icons loses the listed PP.
            // The same tie rule applies.
            if (icons == minIcons) {
                if (preventLoss.get(player)) {
                    System.out.println(player.getNickname() + " has the fewest icons, but a Building protects them from PP loss!");
                } else {
                    // Note: PPtoLose is already negative
                    player.addPP(PPtoLose);
                    System.out.println(player.getNickname() + " loses the ritual and suffers " + PPtoLose + " PP.");
                }
            }
        }
    }



}

