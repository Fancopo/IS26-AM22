package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;

import java.util.List;

public class sustenance implements EventEffect {

    private Era era;

    public sustenance(Era era) {
        this.era = era;
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPLose = 0;

        if (era == Era.I) {
            PPLose = -1;
        } else if (era == Era.II) {
            PPLose = -2;
        } else if (era == Era.III) {
            PPLose = -3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int totalCharacters = tribe.getMembers().size();

            int collectorsCount = tribe.countCharacters(CharacterType.COLLECTOR);
            int totalDiscount = collectorsCount * 3;

            for (Building building : tribe.getBuildings()) {
                BuildingEffect effect = building.getEffect();
                totalDiscount += effect.getSustenanceDiscount(tribe);
            }

            int foodToPay = Math.max(0, totalCharacters - totalDiscount);

            if (player.getFood() >= foodToPay) {
                player.addFood(-foodToPay);
                System.out.println(player.getNickname() + " paga " + foodToPay + " cibo e sfama tutta la tribù.");

            } else {
                int foodAvailable = player.getFood();
                int unfedCharacters = foodToPay - foodAvailable;

                player.addFood(-foodAvailable);

                int totalPenalty = PPLose * unfedCharacters;
                player.addPP(totalPenalty);

                System.out.println(player.getNickname() + " ha solo " + foodAvailable +
                        " cibo. Non riesce a sfamare " + unfedCharacters +
                        " personaggi e subisce " + totalPenalty + " PP di penalità.");
            }
        }
    }
}