package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

public class Artist extends TribeCharacter implements CharacterEffect {

    public Artist(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.ARTIST, null);
        setEffect(this);
    }

    @Override
    public String describe() {
        return "Artist: scores prestige points during the Cave Paintings event "
                + "(more artists in your tribe -> more PP; too few = -2 PP penalty).";
    }
}
