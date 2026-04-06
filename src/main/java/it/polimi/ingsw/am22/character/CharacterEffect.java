package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import javafx.application.Application;

public interface CharacterEffect {
    void applyImmediateEffect(Player player, Tribe tribe);
    void addCharacter(Player player, Tribe tribe);
    public int getNumStars();
    public char getIconPerInventor();
    public int getDiscountFood();
    public int getPP();
}
