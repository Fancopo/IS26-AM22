package it.polimi.ingsw.am22.Building;

import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;

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
                if (character.getCharacterType() == CharacterType.BUILDER) {

                    // Add their base PP to the extra total
                    extraBuilderPP += character.getPP();
                }
            }

            total += extraBuilderPP;
        }

        return total;
    }
}
