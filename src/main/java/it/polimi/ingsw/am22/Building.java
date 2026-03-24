package it.polimi.ingsw.am22;

import java.util.HashMap;
import java.util.Map;

public class Building extends Card {
    private int foodPrice;
    private int finalPP;
    private boolean currentApplicable;
    private BuildingEffect effect;

    public Building(Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect) {
        super(era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
        this.currentApplicable = true;
        this.effect = effect;
    }

    public void ApplyEffect() {
    }

    public int getFoodPrice() { return foodPrice; }
    public int getFinalPP() { return finalPP; }
    public boolean isCurrentApplicable() { return currentApplicable; }
    public BuildingEffect getEffect() { return effect; }
}



// 1. EndGameScoringEffect
public class EndGameScoringEffect implements BuildingEffect {
    private int flatPP;
    private int pointsPerSet;
    private CharacterType targetCharacterType;
    private int multiplierPP;
    private boolean doubleBuilderPP;

    public EndGameScoringEffect(int flatPP, int pointsPerSet, CharacterType targetCharacterType, int multiplierPP, boolean doubleBuilderPP) {
        this.flatPP = flatPP;
        this.pointsPerSet = pointsPerSet;
        this.targetCharacterType = targetCharacterType;
        this.multiplierPP = multiplierPP;
        this.doubleBuilderPP = doubleBuilderPP;
    }

    @Override
    public int calculateEndGame(Tribe tribe) {
        // Add flat points (25 PP card)
        int total = flatPP;

        // Sets of 6 unique types
        if (pointsPerSet > 0) {
            int completeSets = Integer.MAX_VALUE; // Start with a huge number

            // Check every single character type (Inventor, Builder, etc.)
            for (CharacterType type : CharacterType.values()) {

                // Count how many the player has of this specific type
                int count = tribe.countCharacters(type);

                // If this count is the lowest we've seen so far, it becomes the new bottleneck
                if (count < completeSets) {
                    completeSets = count;
                }
            }

            // Safety fallback
            if (completeSets == Integer.MAX_VALUE) {
                completeSets = 0;
            }

            // Multiply and add to total
            total += (completeSets * pointsPerSet);
        }

        // Multiplier based on character type count
        if (targetCharacterType != null && multiplierPP > 0) {
            total += (tribe.countCharacters(targetCharacterType) * multiplierPP);
        }

        // Double Builder PP
        if (doubleBuilderPP) {
            int extraBuilderPP = 0;

            // Loop through every member of the tribe
            for (TribeCharacter character : tribe.getMembers()) {

                // Check if the character is a Builder type
                if (character.getCharacterType() == CharacterType.Builder) {

                    // Add their base PP to the extra total
                    extraBuilderPP += character.getPP();
                }
            }

            total += extraBuilderPP;
        }

        return total;
    }
}




// 2. ShamanicModifierEffect
public class ShamanicModifierEffect implements BuildingEffect {
    private int extraIcons;
    private boolean preventPPLoss;
    private boolean doubleWinPP;

    public ShamanicModifierEffect(int extraIcons, boolean preventPPLoss, boolean doubleWinPP) {
        this.extraIcons = extraIcons;
        this.preventPPLoss = preventPPLoss;
        this.doubleWinPP = doubleWinPP;
    }

    @Override
    public void modifyShamanicRitual() {
        // The game engine will fetch these flags when resolving the event
    }

    // Getters for the engine to read
    public int getExtraIcons() { return extraIcons; }
    public boolean isPreventPPLoss() { return preventPPLoss; }
    public boolean isDoubleWinPP() { return doubleWinPP; }
}





// 3. SustenanceDiscountEffect
public class SustenanceDiscountEffect implements BuildingEffect {
    private CharacterType targetCharacterType;

    public SustenanceDiscountEffect(CharacterType targetCharacterType) {
        this.targetCharacterType = targetCharacterType;
    }

    @Override
    public int getSustenanceDiscount(Tribe tribe) {
        // Returns 1 food discount for every character matching the target type
        return tribe.countCharacters(targetCharacterType);
    }
}



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
                    char icon = character.getIconpetentInventor();
                    iconCounts.put(icon, iconCounts.getOrDefault(icon, 0) + 1);
                }
            }

            int pairs = 0;
            for (int count : iconCounts.values()) {
                pairs += (count / 2);
            }

            currentMatches = pairs;

        // --- CONDITION 2: The Set of 6 ---
        else if (conditionType == CollectionCondition.SET_OF_6) {
            // Start with the highest possible number
            int minSets = Integer.MAX_VALUE;

            // Loop through every single character type (Inventor, Builder, etc.)
            for (CharacterType type : CharacterType.values()) {

                // Ask the Tribe how many characters of this specific type it currently has
                int count = tribe.countCharacters(type);

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




// 5. EventYieldBonusEffect
public class EventYieldBonusEffect implements BuildingEffect {
    private EventType targetEventType;
    private int bonusFood;
    private int bonusPP;

    public EventYieldBonusEffect(EventType targetEventType, int bonusFood, int bonusPP) {
        this.targetEventType = targetEventType;
        this.bonusFood = bonusFood;
        this.bonusPP = bonusPP;
    }

    @Override
    public void modifyEventYield() {
        // The game engine will fetch these bonuses when the specific event occurs
    }

    public EventType getTargetEventType() { return targetEventType; }
    public int getBonusFood() { return bonusFood; }
    public int getBonusPP() { return bonusPP; }
}




// 6. TurnPhaseModifierEffect
public class TurnPhaseModifierEffect implements BuildingEffect {
    private boolean extraFoodOnTurnOrderBonus;
    private boolean extraBuyAtRoundEnd;

    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

    @Override
    public void onTotemPlaced() {
        // Logic executed by the Game class when a totem is placed
    }

    @Override
    public void onRoundEnd() {
        // Logic executed by the Game class during the cleanup phase
    }

    public boolean isExtraFoodOnTurnOrderBonus() { return extraFoodOnTurnOrderBonus; }
    public boolean isExtraBuyAtRoundEnd() { return extraBuyAtRoundEnd; }
}