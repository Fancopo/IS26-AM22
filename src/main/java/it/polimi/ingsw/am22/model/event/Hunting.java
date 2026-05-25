package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class Hunting extends Event implements EventEffect {

    public Hunting(String id, Era era, int minPlayers, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.HUNTING, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int ppPerHunter = switch (getEra()) {
            case I -> 1;
            case II -> 2;
            case III -> 3;
        };

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int huntersCount = tribe.countCharacters(CharacterType.HUNTER);
            if (huntersCount == 0) continue;

            for (Building building : tribe.getBuildings()) {
                building.getEffect().applyEventBonus(EventType.HUNTING, player, huntersCount);
            }

            player.addFood(huntersCount);
            player.addPP(huntersCount * ppPerHunter);
        }
    }

    @Override
    public String describe() {
        int pp = switch (getEra()) { case I -> 1; case II -> 2; case III -> 3; };
        return "Hunting event (Era " + getEra() + "): each player gains 1 food and "
                + pp + " PP per Hunter in their tribe. "
                + "Events cannot be picked into the tribe.";
    }
}
