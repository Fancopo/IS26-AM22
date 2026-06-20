package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

/**
 * Sustenance event. Each player must pay one food per character in their tribe;
 * Collectors and sustenance-discount Buildings feed some characters for free.
 * Unfed characters cost PP. Resolves after every other event of the round.
 */
public class Sustenance extends Event implements EventEffect {

    private static final int FOOD_PER_COLLECTOR = 3;

    /**
     * @param id          the card id
     * @param era         the Era the card belongs to
     * @param minPlayers  the minimum player count for this card to be in play
     * @param eventEffect ignored; the card registers itself as its own effect
     */
    public Sustenance(String id, Era era, int minPlayers, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SUSTENANCE, eventEffect);
        setEffect(this);
    }

    /**
     * Charges each player food to feed their tribe (after discounts), converting
     * any shortfall into an Era-scaled PP penalty.
     *
     * @param players the players in the game
     * @param id      the id of the resolving event
     */
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
