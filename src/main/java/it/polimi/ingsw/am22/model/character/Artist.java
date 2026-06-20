package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

/**
 * Artist character. Scores prestige points during the Cave Paintings event: the
 * more Artists in the tribe, the more PP, with a penalty if there are too few.
 */
public class Artist extends TribeCharacter implements CharacterEffect {

    /**
     * @param id         the card id
     * @param era        the Era the card belongs to
     * @param minPlayers the minimum player count for this card to be in play
     */
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
