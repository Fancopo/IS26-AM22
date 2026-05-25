package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

public class Shaman extends TribeCharacter implements CharacterEffect {

    private final int numStars;

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
