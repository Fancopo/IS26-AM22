package it.polimi.ingsw.am22.Building;

import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.event.EventType;

public interface BuildingEffect {
    default int calculateEndGame(Tribe tribe) { return 0; }
    default int getExtraShamanIcons() {return 0;}
    default boolean preventsShamanPPLoss() {return false;}
    default boolean doublesShamanWinPP() {return false;}
    default void onTotemPlaced(Player owner) {}
    default boolean hasExtraBuyAtRoundEnd() {return false;} // By default, buildings do NOT give an extra buy!
    default int getSustenanceDiscount(Tribe tribe) { return 0; }
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}
    default void applyEventBonus(EventType eventType, Player player, int characterCount) {}
}
