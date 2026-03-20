package il.polimi.ingse.character;

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
    public void applyEffect(Player player, Tribe tribe){
        tribe.addCharacter(getCharacterType());

    }

}
