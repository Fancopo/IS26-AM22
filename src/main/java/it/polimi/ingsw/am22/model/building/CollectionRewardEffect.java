package it.polimi.ingsw.am22.model.building;

import java.util.HashMap;
import java.util.Map;

import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.character.CharacterType;

/**
 * {@link BuildingEffect} that grants food whenever the owner completes a new
 * "collection" — either a pair of same-icon Inventors or a full set of the six
 * character types. It remembers how many collections were already rewarded so
 * the same one is never paid for twice.
 */
public class CollectionRewardEffect implements BuildingEffect {
    private final CollectionCondition conditionType;
    private final int foodReward;
    private int previousMatches = 0;

    /**
     * @param conditionType the collection that triggers the reward
     * @param foodReward    the food granted for each newly completed collection
     */
    public CollectionRewardEffect(CollectionCondition conditionType, int foodReward) {
        this.conditionType = conditionType;
        this.foodReward = foodReward;
    }

    /**
     * Pays the reward for any collection newly completed by the added character.
     *
     * @param player  the building's owner
     * @param newChar the character just added to the tribe
     */
    @Override
    public void onCharacterAdded(Player player, TribeCharacter newChar) {
        // A missing condition matches nothing, so the effect never grants a reward.
        if (conditionType == null) {
            return;
        }
        int currentMatches = switch (conditionType) {
            case INVENTOR_PAIR -> countInventorPairs(player);
            case SET_OF_6 -> countSetsOfSix(player);
        };

        if (currentMatches > previousMatches) {
            player.addFood(foodReward * (currentMatches - previousMatches));
            previousMatches = currentMatches;
        }
    }

    // Counts how many same-icon Inventor pairs the player currently holds.
    private int countInventorPairs(Player player) {
        Map<Character, Integer> iconCounts = new HashMap<>();
        for (TribeCharacter character : player.getTribe().getMembers()) {
            if (character.getCharacterType() == CharacterType.INVENTOR) {
                char icon = character.getIconPerInventor();
                iconCounts.merge(icon, 1, Integer::sum);
            }
        }

        int pairs = 0;
        for (int count : iconCounts.values()) {
            pairs += count / 2;
        }
        return pairs;
    }

    // The number of complete sets equals the count of the scarcest character type.
    private int countSetsOfSix(Player player) {
        int minCount = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.values()) {
            minCount = Math.min(minCount, player.getTribe().countCharacters(type));
        }
        return minCount;
    }

    @Override
    public String describe() {
        String trigger = switch (conditionType) {
            case INVENTOR_PAIR -> "each new pair of Inventors with the same icon";
            case SET_OF_6      -> "each new complete set of all 6 character types";
        };
        return "Collection reward: grants +" + foodReward + " food for " + trigger + ".";
    }
}
