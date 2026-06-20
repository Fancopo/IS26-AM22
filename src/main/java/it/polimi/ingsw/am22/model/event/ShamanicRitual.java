package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shamanic Ritual event. Sums each player's star icons (from Shamans and
 * star-granting Buildings); the player(s) with the most stars gain PP and the
 * one(s) with the fewest lose PP, both Era-scaled. Buildings may prevent the
 * loss or double the win.
 */
public class ShamanicRitual extends Event implements EventEffect {

    /**
     * @param id          the card id
     * @param era         the Era the card belongs to
     * @param minPlayers  the minimum player count for this card to be in play
     * @param eventEffect ignored; the card registers itself as its own effect
     */
    public ShamanicRitual(String id, Era era, int minPlayers, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SHAMANIC_RITUAL, eventEffect);
        setEffect(this);
    }

    /**
     * Tallies every player's stars and applies the win/loss PP at the extremes.
     *
     * @param players the players in the game
     * @param id      the id of the resolving event
     */
    @Override
    public void applyEvent(List<Player> players, String id) {
        int ppToAdd = switch (getEra()) {
            case I -> 5;
            case II -> 10;
            case III -> 15;
        };
        int ppToLose = switch (getEra()) {
            case I -> -3;
            case II -> -5;
            case III -> -7;
        };

        Map<Player, Integer> totalIcons = new HashMap<>();
        Map<Player, Boolean> preventLoss = new HashMap<>();
        Map<Player, Boolean> doubleWin = new HashMap<>();

        for (Player player : players) {
            int icons = 0;
            boolean noLoss = false;
            boolean doublePP = false;

            Tribe tribe = player.getTribe();
            if (tribe != null) {
                for (TribeCharacter character : tribe.getMembers()) {
                    icons += character.getNumStars();
                }
                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                    icons += effect.getExtraShamanIcons();
                    if (effect.preventsShamanPPLoss()) noLoss = true;
                    if (effect.doublesShamanWinPP()) doublePP = true;
                }
            }

            totalIcons.put(player, icons);
            preventLoss.put(player, noLoss);
            doubleWin.put(player, doublePP);
        }

        if (totalIcons.isEmpty()) return;

        int maxIcons = Collections.max(totalIcons.values());
        int minIcons = Collections.min(totalIcons.values());

        for (Player player : players) {
            int icons = totalIcons.get(player);
            if (icons == maxIcons) {
                player.addPP(doubleWin.get(player) ? ppToAdd * 2 : ppToAdd);
            }
            if (icons == minIcons && !preventLoss.get(player)) {
                player.addPP(ppToLose);
            }
        }
    }

    @Override
    public String describe() {
        int win  = switch (getEra()) { case I -> 5;  case II -> 10; case III -> 15; };
        int lose = switch (getEra()) { case I -> -3; case II -> -5; case III -> -7; };
        return "Shamanic Ritual event (Era " + getEra() + "): player(s) with the most "
                + "star icons gain " + win + " PP; player(s) with the fewest lose "
                + lose + " PP. Buildings can grant extra stars, prevent the loss, "
                + "or double the win. Events cannot be picked into the tribe.";
    }
}
