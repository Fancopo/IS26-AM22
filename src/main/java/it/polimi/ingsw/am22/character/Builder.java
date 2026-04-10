package it.polimi.ingsw.am22.character;


import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;


public class Builder extends TribeCharacter implements CharacterEffect {
    private int DiscountFood;
    private int PP;

    public Builder(String id, Era era, int minPlayers, String characterType, int discountFood, int PP){
        super(id, era, minPlayers, CharacterType.BUILDER);
        this.DiscountFood = discountFood;
        this.PP = PP;
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
