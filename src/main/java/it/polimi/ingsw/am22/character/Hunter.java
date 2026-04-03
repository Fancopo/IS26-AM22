package it.polimi.ingsw.am22.character;

import javafx.application.Application;

public class Hunter extends TribeCharacter implements CharacterEffect{
    private final boolean hasFoodIcon;

    public Hunter(char id, String type, int era, int minPlayers, String characterType, boolean hasFoodIcon){
        super(id, type, era, minPlayers, "Hunter");
        this.hasFoodIcon = hasFoodIcon;
    }

    @Override
    public void applyEffect(Player player, Tribe tribe){

        tribe.addCharacter(getCharacterType());
        if(this.hasFoodIcon){
            int foodToAdd = tribe.countCharacter(getCharacterType());
            player.addFood(foodToAdd);
        }
    }
}
