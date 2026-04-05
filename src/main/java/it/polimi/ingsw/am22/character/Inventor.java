package it.polimi.ingse.character;

import javafx.application.Application;

public class Inventor extends TribeCharacter implements CharacterEffect{

    private final char IconPerInventor;

    public Inventor(char id, String type, int era, int minPlayers, String characterType, char IconPerInventor){
        super(id, type, era, minPlayers, "Inventor");

        this.IconPerInventor = IconPerInventor;
    }


    public char getIconPerInventor(){
        return IconPerInventor;
    }

    @Override
    public int getProvidedIcons() {
        // L'inventore restituisce il suo valore specifico!
        return this.IconPerInventor;
    }


    @Override
    public void applyImmediateEffectEffect(Player player, Tribe tribe){}

}
