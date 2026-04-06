package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import javafx.application.Application;

public class Builder extends TribeCharacter implements CharacterEffect {
    private int DiscountFood;
    private int PP;

    public Builder(char id, String type, int era, int minPlayers, String characterType, int discountFood, int PP){
        super(id, type, era, minPlayers, "Builder");
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
    public void applyImmediateEffectEffect(Player player, Tribe tribe){}
}
