package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import javafx.application.Application;

public class Inventor extends TribeCharacter implements CharacterEffect{

    private final char icon;

    public Inventor(String id, Era era, int minPlayers, String characterType, char icon){
        super(id, era, minPlayers, CharacterType.INVENTOR);

        this.icon = icon;
    }


    public char getIcon(){
        return icon;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}
}
