package it.polimi.ingsw.am22.character;

import javafx.application.Application;

public class Hunter extends TribeCharacter implements CharacterEffect{
    private final boolean HasFoodIcon;

    public Hunter(char id, String type, int era, int minPlayers, String characterType, boolean hasFoodIcon){
        super(id, type, era, minPlayers, "Hunter");
        this.HasFoodIcon = HasFoodIcon;
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){

        if(this.HasFoodIcon){
            int foodToAdd = player.getTribe().countCharacter(getCharacterType());
            player.addFood(foodToAdd);
            System.out.println("Cacciatore con icona giocato! Aggiunto " + hunterCount + " cibo.");
        }
    }

}
