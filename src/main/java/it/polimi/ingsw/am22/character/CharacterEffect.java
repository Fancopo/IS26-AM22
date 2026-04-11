package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;


public interface CharacterEffect {
    void applyImmediateEffect(Player player, Tribe tribe);
    public int getNumStars();
    public char getIconPerInventor();
    public int getDiscountFood();
    public int getPP();
}
