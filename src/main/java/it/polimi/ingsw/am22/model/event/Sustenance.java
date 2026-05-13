package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class Sustenance extends Event implements EventEffect {

    private static final int FOOD_PER_COLLECTOR = 3;

    public Sustenance(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SUSTENANCE, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int ppPenaltyPerUnfed = switch (getEra()) {
            case I -> -1;
            case II -> -2;
            case III -> -3;
        };

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int totalCharacters = tribe.getMembers().size();
            int discount = tribe.countCharacters(CharacterType.COLLECTOR) * FOOD_PER_COLLECTOR;
            for (Building building : tribe.getBuildings()) {
                discount += building.getEffect().getSustenanceDiscount(tribe);
            }

            int foodToPay = Math.max(0, totalCharacters - discount);
            int foodAvailable = player.getFood();

            if (foodAvailable >= foodToPay) {
                player.addFood(-foodToPay);
            } else {
                int unfed = foodToPay - foodAvailable;
                player.addFood(-foodAvailable);
                player.addPP(ppPenaltyPerUnfed * unfed);
            }
        }
    }
}
