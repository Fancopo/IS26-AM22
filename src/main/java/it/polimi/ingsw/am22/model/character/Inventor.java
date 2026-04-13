package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;

public class Inventor extends TribeCharacter implements CharacterEffect{

    private final char IconPerInventor;

    public Inventor(String id, Era era, int minPlayers, char iconPerInventor) {
        super(id, era, minPlayers, CharacterType.INVENTOR, null);
        this.IconPerInventor = iconPerInventor;
        setEffect(this);
    }


    public char getIconPerInventor(){
        return IconPerInventor;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}

}
