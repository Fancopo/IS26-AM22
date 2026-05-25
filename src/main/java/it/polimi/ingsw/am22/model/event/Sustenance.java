package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class Sustenance extends Event implements EventEffect {

    private static final int FOOD_PER_COLLECTOR = 3;

    public Sustenance(String id, Era era, int minPlayers, EventEffect eventEffect) {
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

    @Override
    public String describe() {
        int penalty = switch (getEra()) { case I -> -1; case II -> -2; case III -> -3; };
        return "Sustenance event (Era " + getEra() + "): each player must pay 1 food per "
                + "character in the tribe (each Collector and Sustenance-discount Building "
                + "feeds " + FOOD_PER_COLLECTOR + " / 1 characters for free). Unfed "
                + "characters cost " + penalty + " PP each. Resolves after every other "
                + "event of the round. Events cannot be picked into the tribe.";
    }
}
