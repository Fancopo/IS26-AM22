package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Builder implements CharacterEffect {

    private int discountFood;
    private int pp;

    public Builder() {
    }

    public Builder(int discountFood, int pp) {
        this.discountFood = discountFood;
        this.pp = pp;
    }

    public void setDiscountFood(int discountFood) {
        this.discountFood = discountFood;
    }

    public void setPp(int pp) {
        this.pp = pp;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

    @Override
    public int getNumStars() {
        return 0;
    }

    @Override
    public char getIconPerInventor() {
        return '0';
    }

    @Override
    public int getDiscountFood() {
        return discountFood;
    }

    @Override
    public int getPP() {
        return pp;
    }
}