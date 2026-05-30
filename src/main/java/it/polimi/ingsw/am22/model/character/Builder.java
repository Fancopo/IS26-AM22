package it.polimi.ingsw.am22.model.character;


import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.PickSimulation;


public class Builder extends TribeCharacter implements CharacterEffect {
    private final int discountFood;
    private final int PP;

    public Builder(String id, Era era, int minPlayers, int discountFood, int PP) {
        super(id, era, minPlayers, CharacterType.BUILDER, null);
        this.discountFood = discountFood;
        this.PP = PP;
        setEffect(this);
    }

    @Override
    public int getDiscountFood() {
        return discountFood;
    }

    @Override
    public int getPP() {
        return PP;
    }

    /** Validation: a Builder picked earlier in the sequence reduces the cost
     *  of any Building picked later. */
    @Override
    public void applyPickEffect(PickSimulation sim) {
        sim.addBuilderDiscount(discountFood);
    }

    @Override
    public String describe() {
        return "Builder: gives -" + discountFood + " food discount on Buildings picked "
                + "after it in the same turn, and contributes " + PP + " PP at end of game.";
    }
}
