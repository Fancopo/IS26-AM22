package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;

import java.util.List;

public class hunting implements EventEffect {

    private Era era;

    public hunting(Era era) {
        this.era = era;
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPperHunter = 0;

        if (era == Era.I) {
            PPperHunter = 1;
        } else if (era == Era.II) {
            PPperHunter = 2;
        } else if (era == Era.III) {
            PPperHunter = 3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int huntersCount = tribe.countCharacters(CharacterType.HUNTER);

            if (huntersCount > 0) {
                int foodPerHunter = 1;
                for (Building building : player.getTribe().getBuildings()) {
                    building.getEffect().applyEventBonus(EventType.HUNTING, player, huntersCount);
                }

                int totalFoodToAdd = huntersCount * foodPerHunter;
                int totalPPToAdd = huntersCount * PPperHunter;

                player.addFood(totalFoodToAdd);
                player.addPP(totalPPToAdd);

                System.out.println(player.getNickname() + " ha " + huntersCount +
                        " Cacciatori. Ottiene " + totalFoodToAdd +
                        " Cibo e " + totalPPToAdd + " PP dalla Caccia!");
            } else {
                System.out.println(player.getNickname() + " non ha Cacciatori e non ottiene nulla.");
            }
        }
    }
}