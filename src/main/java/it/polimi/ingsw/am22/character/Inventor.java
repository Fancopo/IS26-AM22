package it.polimi.ingsw.am22.character;

import javafx.application.Application;

public class Inventor extends TribeCharacter implements CharacterEffect{

    private final char icon;

    public Inventor(char id, String type, int era, int minPlayers, String characterType, char icon){
        super(id, type, era, minPlayers, "Inventor");

        this.icon = icon;
    }


    public char getIcon(){
        return icon;
    }

    @Override
    public void applyEffect(Player player, Tribe tribe){
        tribe.addCharacter(getCharacterType());
    }
}
