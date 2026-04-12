package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Artist extends TribeCharacter implements CharacterEffect {

    public Artist(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.ARTIST, null);
        setEffect(this);
    }
    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

}
