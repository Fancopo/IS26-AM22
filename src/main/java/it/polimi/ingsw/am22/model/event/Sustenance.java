package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;


import java.util.List;


public class Sustenance extends Event implements EventEffect {

    public Sustenance(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SUSTENANCE, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPLose = 0;

        // Penalty values based on the current Era
        if (this.getEra() == Era.I) {
            PPLose = -1;
        } else if (this.getEra() == Era.II) {
            PPLose = -2;
        } else if (this.getEra() == Era.III) {
            PPLose = -3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // 1. Count characters (1 food per character)
            int totalCharacters = tribe.getMembers().size();

            // 2. Compute the base Collector discount (3 food per collector)
            int collectorsCount = tribe.countCharacters(CharacterType.COLLECTOR);
            int totalDiscount = collectorsCount * 3;

            // 3. Add any discounts granted by Buildings
            for (Building building : tribe.getBuildings()) {
                     BuildingEffect effect = building.getEffect();
                    // The building computes its own discount based on the tribe
                    totalDiscount += effect.getSustenanceDiscount(tribe);
            }

            // 4. Final food cost (cannot be negative)
            int foodToPay = Math.max(0, totalCharacters - totalDiscount);

            // 5. Resolve the payment
            if (player.getFood() >= foodToPay) {
                // Has enough food to feed the whole tribe
                player.addFood(-foodToPay); // Subtract from the player's food total
                System.out.println(player.getNickname() + " pays " + foodToPay + " food and feeds the whole tribe.");

            } else {
                // Not enough food: pays everything available
                int foodAvailable = player.getFood();
                int unfedCharacters = foodToPay - foodAvailable;

                // Drain the player's food (pays everything owned)
                player.addFood(-foodAvailable);

                // Compute and apply the penalty (PPLose is already negative)
                int totalPenalty = PPLose * unfedCharacters;
                player.addPP(totalPenalty);

                System.out.println(player.getNickname() + " only has " + foodAvailable +
                        " food. Cannot feed " + unfedCharacters +
                        " characters and suffers " + totalPenalty + " PP penalty.");
            }
        }
    }

}


