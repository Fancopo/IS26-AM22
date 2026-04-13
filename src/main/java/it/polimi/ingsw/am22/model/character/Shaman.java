package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;


public class Shaman extends TribeCharacter implements CharacterEffect{

    private int numStars;

    public Shaman(String id, Era era, int minPlayers, int numStars) {
        super(id, era, minPlayers, CharacterType.SHAMAN, null);
        this.numStars = numStars;
        setEffect(this);
    }

    @Override
    public int getNumStars() {
        return numStars;
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}

}
