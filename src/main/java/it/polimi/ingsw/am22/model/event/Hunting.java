package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class Hunting extends Event implements EventEffect {

    public Hunting(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.HUNTING, eventEffect);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPperHunter = 0;

        if (this.getEra() == Era.I) {
            PPperHunter = 1;
        } else if (this.getEra() == Era.II) {
            PPperHunter = 2;
        } else if (this.getEra() == Era.III) {
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