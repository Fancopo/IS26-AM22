package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;

public class Artist extends TribeCharacter implements CharacterEffect {

    public Artist(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.ARTIST, null);
        setEffect(this);
    }
    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

}
