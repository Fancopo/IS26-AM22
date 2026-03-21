package it.polimi.ingsw.am22;

public enum CharacterType { INVENTOR, BUILDER, GATHERER, SHAMAN, ARTIST, HUNTER, ANY }
public enum EventType { SUSTENANCE, HUNTING, SHAMANIC_RITUAL, CAVE_PAINTINGS }
public enum CollectionCondition { SET_OF_6, INVENTOR_PAIR }

public interface BuildingEffect {
    // Hooks for different phases of the game
    default int calculateEndGamePP(Player player) { return 0; }
    default void modifyShamanicRitual(Player player) {}
    default int getSustenanceDiscount(Player player) { return 0; }
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}
    default void modifyEventYield(Player player, EventType eventType) {}
    default void onTotemPlaced(Player player, boolean spaceHasFoodBonus, boolean isLastSpace) {}
    default void onRoundEnd(Player player) {}
}

public class Building extends Card {
    private int foodPrice;
    private BuildingEffect effect;

    public Building(char id, Era era, int minPlayers, int foodPrice, BuildingEffect effect) {
        super(id, era, minPlayers);
        this.foodPrice = foodPrice;
        this.effect = effect;
    }

    public int getFoodPrice() { return foodPrice; }
    public BuildingEffect getEffect() { return effect; }
}
//1. EndGameScoringEffect
//Handles buildings that only activate at the end of the game to grant Prestige Points (PP).
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
    public int calculateEndGamePP(Player player) {
        int total = flatPP; // Add flat points (e.g., the 25 PP card)

        // Add points for sets of 6
        if (pointsPerSet > 0) {
            int sets = player.getTribe().getUniqueCharacterTypeCount() == 6 ? 1 : 0;
            total += (sets * pointsPerSet);
        }

        // Add multiplied points for specific characters
        if (targetCharacterType != null && multiplierPP > 0) {
            long count = player.getTribe().getMembers().stream()
                    .filter(c -> c.getCharacterType() == targetCharacterType)
                    .count();
            total += (count * multiplierPP);
        }

        // Add double builder points
        if (doubleBuilderPP) {
            // Logic to calculate and add base builder PP again
        }

        return total;
    }
}


//2. ShamanicModifierEffect
//Modifies the rules exclusively during the Shamanic Ritual Event.
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
    public void modifyShamanicRitual(Player player) {
        if (preventPPLoss) player.hasShamanicProtection = true;
        if (doubleWinPP) player.hasShamanDoublePP = true;
        // extraIcons is usually handled by a getter pulled by the engine during the event calculation
    }
}


//3. SustenanceDiscountEffect
//Grants food discounts during the Sustenance Event based on specific character types.
public class SustenanceDiscountEffect implements BuildingEffect {
    private CharacterType targetCharacterType;

    public SustenanceDiscountEffect(CharacterType targetCharacterType) {
        this.targetCharacterType = targetCharacterType;
    }

    @Override
    public int getSustenanceDiscount(Player player) {
        return (int) player.getTribe().getMembers().stream()
                .filter(c -> c.getCharacterType() == targetCharacterType)
                .count(); // 1 Food discount per matching character
    }
}


//4. CollectionRewardEffect
//A reactive listener that grants Food immediately when a specific combination of cards is collected.
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

        if (conditionType == CollectionCondition.SET_OF_6) {
            currentMatches = player.getTribe().getUniqueCharacterTypeCount() == 6 ? 1 : 0;
        } else if (conditionType == CollectionCondition.INVENTOR_PAIR) {
            // Logic to count identical inventor pairs
        }

        if (currentMatches > previousMatches) {
            player.addFood(foodReward * (currentMatches - previousMatches));
            previousMatches = currentMatches;
        }
    }
}


//5. EventYieldBonusEffect
//Passively increases the resources generated by characters during Hunting or Cave Painting events.
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
    public void modifyEventYield(Player player, EventType currentEvent) {
        if (this.targetEventType == currentEvent) {
            if (currentEvent == EventType.HUNTING) {
                long hunters = player.getTribe().getMembers().stream().filter(c -> c.getCharacterType() == CharacterType.HUNTER).count();
                player.addFood((int) hunters * bonusFood);
                player.addPP((int) hunters * bonusPP);
            } else if (currentEvent == EventType.CAVE_PAINTINGS) {
                long artists = player.getTribe().getMembers().stream().filter(c -> c.getCharacterType() == CharacterType.ARTIST).count();
                player.addFood((int) artists * bonusFood);
            }
        }
    }
}


//6. TurnPhaseModifierEffect
//Alters base game rules regarding Totem placement or Round End cleanup.
public class TurnPhaseModifierEffect implements BuildingEffect {
    private boolean extraFoodOnTurnOrderBonus;
    private boolean extraBuyAtRoundEnd;

    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

    @Override
    public void onTotemPlaced(Player player, boolean spaceHasFoodBonus, boolean isLastSpace) {
        if (extraFoodOnTurnOrderBonus && spaceHasFoodBonus && !isLastSpace) {
            player.addFood(1);
        }
    }

    @Override
    public void onRoundEnd(Player player) {
        if (extraBuyAtRoundEnd) {
            player.canTakeExtraCardEndOfRound = true;
        }
    }
}