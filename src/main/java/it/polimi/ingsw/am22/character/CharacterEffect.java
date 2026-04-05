package it.polimi.ingsw.am22.character;

import javafx.application.Application;

public interface CharacterEffect {
    void applyImmediateEffect(Player player, Tribe tribe);
    void addCharacter(Player player, Tribe tribe);
}
