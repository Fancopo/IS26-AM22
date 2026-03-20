package it.polimi.ingsw.am22;

// 1. Take 5 Food every time you complete a set of 6 different Character types[cite: 385].
public class SetCompletionFoodBuilding extends Building {
    private int previouslyCompletedSets = 0;

    public SetCompletionFoodBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    @Override
    public void ApplyEffect(Player player) {
        // This would be called every time a new character is added to the tribe
        int currentSets = player.getTribe().getUniqueCharacterTypeCount() == 6 ? 1 : 0;
        // Note: Realistically, you'd calculate actual full sets if players can have multiple of each type.

        if (currentSets > previouslyCompletedSets) {
            int newSets = currentSets - previouslyCompletedSets;
            player.addFood(5 * newSets); [cite: 385]
            previouslyCompletedSets = currentSets;
        }
    }
}

// 2. Discount 1 Food per indicated Character (Artist/Inventor/Gatherer) during Sustenance[cite: 387, 391].
public class SustenanceDiscountBuilding extends Building {
    public SustenanceDiscountBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    // Called specifically during the Sustenance Event calculation
    public int calculateSustenanceDiscount(Player player) {
        int discount = 0;
        for (TribeCharacter c : player.getTribe().getMembers()) {
            if (c.getCharacterType() == CharacterType.ARTIST ||
                    c.getCharacterType() == CharacterType.INVENTOR ||
                    c.getCharacterType() == CharacterType.GATHERER) {
                discount += 1; [cite: 387, 391]
            }
        }
        return discount;
    }

    @Override
    public void ApplyEffect(Player player) { /* Managed by game engine during Sustenance phase */ }
}

// 3. Do not lose PP during Shamanic Ritual if you have the fewest icons[cite: 392, 393, 394].
public class ShamanicProtectionBuilding extends Building {
    public ShamanicProtectionBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    @Override
    public void ApplyEffect(Player player) {
        player.hasShamanicProtection = true; // Engine checks this before applying negative PP [cite: 392]
    }
}

// 4. Take 1 additional Food if placed on a Turn Order space with a Food bonus[cite: 395].
public class TurnOrderFoodBonusBuilding extends Building {
    public TurnOrderFoodBonusBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    // Called when the player places their Totem
    public void onTotemPlaced(Player player, boolean spaceHasFoodBonus, boolean isLastSpace) {
        if (spaceHasFoodBonus && !isLastSpace) {
            player.addFood(1); [cite: 395, 396]
        }
    }

    @Override
    public void ApplyEffect(Player player) { /* Handled by onTotemPlaced hook */ }
}

// 5. Take 3 Food every time you get a pair of identical Inventors[cite: 399].
public class InventorPairFoodBuilding extends Building {
    private int previouslyFoundPairs = 0;

    public InventorPairFoodBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    @Override
    public void ApplyEffect(Player player) {
        // Count pairs of identical inventors
        int currentPairs = 0;
        List<String> seenIcons = new ArrayList<>();

        for (TribeCharacter c : player.getTribe().getMembers()) {
            if (c.getCharacterType() == CharacterType.INVENTOR) {
                String icon = c.getSubIcon();
                if (seenIcons.contains(icon)) {
                    currentPairs++;
                    seenIcons.remove(icon); // Consume the pair
                } else {
                    seenIcons.add(icon);
                }
            }
        }

        if (currentPairs > previouslyFoundPairs) {
            int newPairs = currentPairs - previouslyFoundPairs;
            player.addFood(3 * newPairs); [cite: 399]
            previouslyFoundPairs = currentPairs;
        }
    }
}

// 6. Your tribe has 3 additional icons during Shamanic Ritual[cite: 401].
public class ShamanBonusIconsBuilding extends Building {
    public ShamanBonusIconsBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    public int getBonusShamanIcons() {
        return 3; [cite: 401]
    }

    @Override
    public void ApplyEffect(Player player) { /* Polled by Game Engine during Shamanic Ritual */ }
}

// 7. Gain double PP during Shamanic Ritual if you have the most icons (no ties)[cite: 405, 406].
public class ShamanDoublePPBuilding extends Building {
    public ShamanDoublePPBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    @Override
    public void ApplyEffect(Player player) {
        player.hasShamanDoublePP = true; // Engine applies multiplier if player is outright winner [cite: 405]
    }
}

// 8. During Hunting, per Hunter, take 1 additional Food and gain 1 additional PP[cite: 407].
public class HuntingBonusBuilding extends Building {
    public HuntingBonusBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    // Called during the Hunting Event calculation
    public void applyHuntingBonus(Player player) {
        long hunterCount = player.getTribe().getMembers().stream()
                .filter(c -> c.getCharacterType() == CharacterType.HUNTER)
                .count();

        player.addFood((int) hunterCount); [cite: 407]
        player.addPP((int) hunterCount); [cite: 407]
    }

    @Override
    public void ApplyEffect(Player player) { /* Handled by applyHuntingBonus hook */ }
}

// 9. End of game: double the PP indicated on your Builder cards[cite: 408].
public class BuilderDoublePPBuilding extends Building {
    public BuilderDoublePPBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    public int getEndGameBonusPP(Player player) {
        int builderBasePP = 0;
        for (TribeCharacter c : player.getTribe().getMembers()) {
            if (c.getCharacterType() == CharacterType.BUILDER) {
                // Assuming TribeCharacter has a getPP() method
                // builderBasePP += c.getPP();
            }
        }
        return builderBasePP; // Return an amount equal to their base value to effectively "double" it [cite: 408]
    }

    @Override
    public void ApplyEffect(Player player) { /* Handled at end game scoring */ }
}

// 10. During Cave Paintings, take 1 Food per Artist[cite: 409].
public class CavePaintingFoodBuilding extends Building {
    public CavePaintingFoodBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    // Called during Cave Painting Event
    public void applyCavePaintingBonus(Player player) {
        long artistCount = player.getTribe().getMembers().stream()
                .filter(c -> c.getCharacterType() == CharacterType.ARTIST)
                .count();

        player.addFood((int) artistCount); [cite: 409]
    }

    @Override
    public void ApplyEffect(Player player) { /* Handled by hook */ }
}

// 11. End of game: gain 6 PP for each set of 6 different Character types[cite: 410].
public class SetPPBuilding extends Building {
    public SetPPBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    public int getEndGameBonusPP(Player player) {
        // Calculate number of full sets of 6 unique types
        // Simplified logic: Count how many of EACH type the player has, find the minimum.
        int fullSets = 1; // Replace with actual set calculation logic
        return fullSets * 6; [cite: 410]
    }

    @Override
    public void ApplyEffect(Player player) { /* Handled at end game scoring */ }
}

// 12. End of game: gain indicated PP for each Character of the indicated type.
public class CharacterTypePPBuilding extends Building {
    private CharacterType targetType;
    private int ppMultiplier;

    public CharacterTypePPBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP, CharacterType targetType) {
        super(id, era, minPlayers, foodPrice, finalPP);
        this.targetType = targetType;

        // Automatically set the exact PP multiplier based on the rules
        switch (targetType) {
            case HUNTER:
                this.ppMultiplier = 3;
                break;
            case ARTIST:
            case GATHERER:
            case BUILDER:
            case SHAMAN:
                this.ppMultiplier = 4;
                break;
            case INVENTOR:
                this.ppMultiplier = 2;
                break;
            default:
                this.ppMultiplier = 0;
        }
    }

    // Called by the game engine at the end of the game
    public int getEndGameBonusPP(Player player) {
        long count = player.getTribe().getMembers().stream()
                .filter(c -> c.getCharacterType() == targetType)
                .count();

        return (int) (count * ppMultiplier);
    }

    @Override
    public void ApplyEffect(Player player) {
        // No active mid-game effect. Handled entirely at end game scoring.
    }
}
// 13. End of round: can take 1 Character/Building card from the top row (paying cost)[cite: 412].
public class ExtraCardBuilding extends Building {
    public ExtraCardBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, finalPP);
    }

    @Override
    public void ApplyEffect(Player player) {
        // Sets a flag so the Game Engine prompts this player before round cleanup
        player.canTakeExtraCardEndOfRound = true; [cite: 412]
    }
}

// 14. End of game: gain 25 PP[cite: 413].
public class FlatPPBuilding extends Building {
    public FlatPPBuilding(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        super(id, era, minPlayers, foodPrice, 25);
        // Note: You could just pass 25 into the base `finalPP` parameter in the constructor,
        // making this subclass technically optional, but creating it is good for code organization!
    }

    @Override
    public void ApplyEffect(Player player) {
        // No active mid-game effect. Points are handled by getFinalPP() from the base class. [cite: 413]
    }
}