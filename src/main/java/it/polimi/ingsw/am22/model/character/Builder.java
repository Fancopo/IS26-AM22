package it.polimi.ingsw.am22.model.character;


import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;


public class Builder extends TribeCharacter implements CharacterEffect {
    private int DiscountFood;
    private int PP;

    public Builder(String id, Era era, int minPlayers, int discountFood, int PP) {
        super(id, era, minPlayers, CharacterType.BUILDER, null);
        this.DiscountFood = discountFood;
        this.PP = PP;
        setEffect(this);
    }


    public int getDiscountFood(){
        return DiscountFood;
    }

    public int getPP(){
        return PP;
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}
}
