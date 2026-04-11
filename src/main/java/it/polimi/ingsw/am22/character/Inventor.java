package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Inventor extends TribeCharacter implements CharacterEffect{

    private final char IconPerInventor;

    public Inventor(String id, Era era, int minPlayers, String characterType, char IconPerInventor){
        super(id, era, minPlayers, CharacterType.INVENTOR);

        this.IconPerInventor = IconPerInventor;
    }


    public char getIconPerInventor(){
        return IconPerInventor;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}

}
