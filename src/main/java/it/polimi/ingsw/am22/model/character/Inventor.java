package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

public class Inventor extends TribeCharacter implements CharacterEffect {

    private final char iconPerInventor;

    public Inventor(String id, Era era, int minPlayers, char iconPerInventor) {
        super(id, era, minPlayers, CharacterType.INVENTOR, null);
        this.iconPerInventor = iconPerInventor;
        setEffect(this);
    }

    @Override
    public char getIconPerInventor() {
        return iconPerInventor;
    }
}
