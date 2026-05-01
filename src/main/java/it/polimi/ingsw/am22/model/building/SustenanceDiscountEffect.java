package it.polimi.ingsw.am22.model.building;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

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
