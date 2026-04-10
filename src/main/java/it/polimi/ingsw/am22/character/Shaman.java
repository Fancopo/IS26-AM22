package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;


public class Shaman extends TribeCharacter implements CharacterEffect{

    private int numStars;

    public Shaman(String id, Era era, int minPlayers, String characterType, int numStars){

        super(id, era, minPlayers, CharacterType.SHAMAN);

        this.numStars = numStars;
    }

    public int getNumStars() {
        return numStars;
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}

}
