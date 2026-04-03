package it.polimi.ingsw.am22.character;

import javafx.application.Application;

public class Builder extends TribeCharacter implements CharacterEffect {
    private int discountFood;
    private int PP;

    public Builder(char id, String type, int era, int minPlayers, String characterType, int discountFood, int PP){
        super(id, type, era, minPlayers, "Builder");
        this.discountFood = discountFood;
        this.PP = PP;
    }

    public int getDiscount(){
        return discountFood;
    }

    public int getPP(){
        return PP;
    }

    @Override
    public void applyEffect(Player player, Tribe tribe){
        tribe.addCharacter(getCharacterType());
    }
}
