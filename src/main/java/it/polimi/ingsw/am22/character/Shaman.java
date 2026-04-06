package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import javafx.application.Application;

public class Shaman extends TribeCharacter implements CharacterEffect{

    private int numStars;

    public Shaman(char id, String type, int era, int minPlayers,String characterType, int numStars){

        super(id, type, era, minPlayers, "Shaman");

        this.numStars = numStars;
    }

    public int getNumStars() {
        return numStars;
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe){}

}
