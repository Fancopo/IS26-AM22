package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Artist implements CharacterEffect {

    public Artist() {
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
        // no immediate effect
    }

    @Override
    public int getNumStars() {
        return 0;
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