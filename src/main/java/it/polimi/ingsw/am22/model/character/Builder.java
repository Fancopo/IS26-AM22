package it.polimi.ingsw.am22.model.character;


import it.polimi.ingsw.am22.model.Era;


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
}
