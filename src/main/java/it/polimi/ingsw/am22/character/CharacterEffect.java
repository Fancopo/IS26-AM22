package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public interface CharacterEffect {
    void applyImmediateEffect(Player player, Tribe tribe);
    int getNumStars();
    char getIconPerInventor();
    int getDiscountFood();
    int getPP();
}