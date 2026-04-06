package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Artist extends TribeCharacter implements CharacterEffect {

    public Artist(char id, String type, int era, int minPlayers, String characterType){
        super(id, type, era, minPlayers, "Artist");
    }


    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }
}
