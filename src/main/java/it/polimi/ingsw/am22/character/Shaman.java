package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Shaman implements CharacterEffect {

    private int numStars;

    public Shaman() {
    }

    public Shaman(int numStars) {
        this.numStars = numStars;
    }

    public void setNumStars(int numStars) {
        this.numStars = numStars;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

    @Override
    public int getNumStars() {
        return numStars;
    }

    @Override
    public char getIconPerInventor() {
        return '0';
    }

    @Override
    public int getDiscountFood() {
        return 0;
    }

    @Override
    public int getPP() {
        return 0;
    }
}