package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;

public class EndGameScoringEffect implements BuildingEffect {
    private final int flatPP;
    private final int pointsPerSet;
    private final CharacterType targetCharacterType;
    private final int multiplierPP;
    private final boolean doubleBuilderPP;

    public EndGameScoringEffect(int flatPP, int pointsPerSet, CharacterType targetCharacterType, int multiplierPP, boolean doubleBuilderPP) {
        this.flatPP = flatPP;
        this.pointsPerSet = pointsPerSet;
        this.targetCharacterType = targetCharacterType;
        this.multiplierPP = multiplierPP;
        this.doubleBuilderPP = doubleBuilderPP;
    }

    @Override
    public int calculateEndGame(Tribe tribe) {
        int total = flatPP;

        if (pointsPerSet > 0) {
            total += completeSetsOfSix(tribe) * pointsPerSet;
        }

        if (targetCharacterType != null && multiplierPP > 0) {
            total += tribe.countCharacters(targetCharacterType) * multiplierPP;
        }

        if (doubleBuilderPP) {
            for (TribeCharacter character : tribe.getMembers()) {
                if (character.getCharacterType() == CharacterType.BUILDER) {
                    total += character.getPP();
                }
            }
        }

        return total;
    }

    private int completeSetsOfSix(Tribe tribe) {
        int minCount = Integer.MAX_VALUE;
        for (CharacterType type : CharacterType.values()) {
            minCount = Math.min(minCount, tribe.countCharacters(type));
        }
        return minCount;
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("End-game scoring:");
        if (flatPP != 0)                     sb.append(" +").append(flatPP).append(" PP flat;");
        if (pointsPerSet > 0)                sb.append(" +").append(pointsPerSet).append(" PP per complete set of 6;");
        if (targetCharacterType != null && multiplierPP > 0) sb.append(" +").append(multiplierPP)
                                                              .append(" PP per ").append(targetCharacterType).append(";");
        if (doubleBuilderPP)                 sb.append(" doubles your Builders' PP;");
        return sb.toString();
    }
}
