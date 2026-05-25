package it.polimi.ingsw.am22.model.building;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

public class SustenanceDiscountEffect implements BuildingEffect {
    private CharacterType targetCharacterType;

    public SustenanceDiscountEffect(CharacterType targetCharacterType) {
        this.targetCharacterType = targetCharacterType;
    }

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
