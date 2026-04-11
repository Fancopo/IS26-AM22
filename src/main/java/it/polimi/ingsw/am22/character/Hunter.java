package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Hunter implements CharacterEffect {

    private boolean hasFoodIcon;

    public Hunter() {
    }

    public Hunter(boolean hasFoodIcon) {
        this.hasFoodIcon = hasFoodIcon;
    }

    public boolean isHasFoodIcon() {
        return hasFoodIcon;
    }

    public void setHasFoodIcon(boolean hasFoodIcon) {
        this.hasFoodIcon = hasFoodIcon;
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
        if (hasFoodIcon) {
            int hunters = tribe.countCharacters(CharacterType.HUNTER);
            player.addFood(hunters);
        }
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