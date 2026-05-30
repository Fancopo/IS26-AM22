package it.polimi.ingsw.am22.model.building;

import java.util.HashMap;
import java.util.Map;

import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.character.CharacterType;

public class CollectionRewardEffect implements BuildingEffect {
    private final CollectionCondition conditionType;
    private final int foodReward;
    private int previousMatches = 0;

    public CollectionRewardEffect(CollectionCondition conditionType, int foodReward) {
        this.conditionType = conditionType;
        this.foodReward = foodReward;
    }

    @Override
    public void onCharacterAdded(Player player, TribeCharacter newChar) {
        int currentMatches = switch (conditionType) {
            case INVENTOR_PAIR -> countInventorPairs(player);
            case SET_OF_6 -> countSetsOfSix(player);
        };

        if (currentMatches > previousMatches) {
            player.addFood(foodReward * (currentMatches - previousMatches));
            previousMatches = currentMatches;
        }
    }

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
