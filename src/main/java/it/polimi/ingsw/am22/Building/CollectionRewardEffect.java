package it.polimi.ingsw.am22.Building;

import java.util.HashMap;
import java.util.Map;

import Building.CollectionCondition;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.Inventor;

// 4. CollectionRewardEffect
public class CollectionRewardEffect implements BuildingEffect {
    private CollectionCondition conditionType;
    private int foodReward;
    private int previousMatches = 0;

    public CollectionRewardEffect(CollectionCondition conditionType, int foodReward) {
        this.conditionType = conditionType;
        this.foodReward = foodReward;
    }

    @Override
    public void onCharacterAdded(Player player, TribeCharacter newChar) {
        int currentMatches = 0;

        // --- CONDITION 1: Pair of Inventors ---
        if (conditionType == CollectionCondition.INVENTOR_PAIR) {
            Map<Character, Integer> iconCounts = new HashMap<>();

            // Get the members list using the standard UML getter
            for (TribeCharacter character : player.getTribe().getMembers()) {
                if (character.getCharacterType() == CharacterType.INVENTOR) {
                    char icon = character.getIcon();
                    iconCounts.put(icon, iconCounts.getOrDefault(icon, 0) + 1);
                }
            }

            int pairs = 0;
            for (int count : iconCounts.values()) {
                pairs += (count / 2);
            }

            currentMatches = pairs;
        }

            // --- CONDITION 2: The Set of 6 ---
        else if (conditionType == CollectionCondition.SET_OF_6) {
                // Start with the highest possible number
                int minSets = Integer.MAX_VALUE;

                // Loop through every single character type (Inventor, Builder, etc.)
                for (CharacterType type : CharacterType.values()) {

                    // Ask the Tribe how many characters of this specific type it currently has
                    int count = Tribe.countCharacters(type);

                    // If this count is the lowest we have seen so far, it becomes our new bottleneck
                    if (count < minSets) {
                        minSets = count;
                    }
                }

                // Safety check: if the enum is empty for some reason, prevent it from staying at MAX_VALUE
                if (minSets == Integer.MAX_VALUE) {
                    minSets = 0;
                }

                // The number of complete sets is equal to the quantity of your scarcest character
                currentMatches = minSets;
            }

            if (currentMatches > previousMatches) {
                player.addFood(foodReward * (currentMatches - previousMatches));
                previousMatches = currentMatches;
            }
        }
    }
