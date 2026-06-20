package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

/**
 * Shaman character. Contributes star icons toward the Shamanic Ritual event,
 * where the most stars win PP and the fewest lose PP.
 */
public class Shaman extends TribeCharacter implements CharacterEffect {

    private final int numStars;

    /**
     * @param id         the card id
     * @param era        the Era the card belongs to
     * @param minPlayers the minimum player count for this card to be in play
     * @param numStars   the number of star icons this Shaman contributes
     */
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
    public String describe() {
        return "Shaman: contributes " + numStars + " star icon"
                + (numStars == 1 ? "" : "s") + " toward the Shamanic Ritual event "
                + "(most stars wins PP, fewest stars loses PP).";
    }
}
