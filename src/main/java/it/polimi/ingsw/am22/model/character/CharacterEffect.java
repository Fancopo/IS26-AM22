package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;


public interface CharacterEffect {
    void applyImmediateEffect(Player player, Tribe tribe);
    public int getNumStars();
    public char getIconPerInventor();
    public int getDiscountFood();
    public int getPP();
}
