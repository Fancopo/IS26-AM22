package it.polimi.ingsw.am22.model.building;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

/**
 * {@link BuildingEffect} that lets the owner feed one extra character during the
 * Sustenance event for each character of a target type in their tribe.
 */
public class SustenanceDiscountEffect implements BuildingEffect {
    private final CharacterType targetCharacterType;

    /**
     * @param targetCharacterType the character type whose count sets the discount
     */
    public SustenanceDiscountEffect(CharacterType targetCharacterType) {
        this.targetCharacterType = targetCharacterType;
    }

    /**
     * @param tribe the owner's tribe
     * @return the number of target-type characters, i.e. the sustenance discount
     */
    @Override
    public int getSustenanceDiscount(Tribe tribe) {
        return tribe.countCharacters(targetCharacterType);
    }

    @Override
    public String describe() {
        return "Sustenance discount: during the Sustenance event you feed 1 extra "
                + "character per " + targetCharacterType + " in your tribe.";
    }
}
