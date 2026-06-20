package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;

/**
 * Inventor character. Each Inventor carries an icon; pairing Inventors with the
 * same icon triggers Inventor-pair rewards and contributes to end-game scoring.
 */
public class Inventor extends TribeCharacter implements CharacterEffect {

    private final char iconPerInventor;

    /**
     * @param id              the card id
     * @param era             the Era the card belongs to
     * @param minPlayers      the minimum player count for this card to be in play
     * @param iconPerInventor the invention icon carried by this Inventor
     */
    public Inventor(String id, Era era, int minPlayers, char iconPerInventor) {
        super(id, era, minPlayers, CharacterType.INVENTOR, null);
        this.iconPerInventor = iconPerInventor;
        setEffect(this);
    }

    @Override
    public char getIconPerInventor() {
        return iconPerInventor;
    }

    /**
     * Each Inventor card has a unique icon (A, B, C, ...) used by collection
     * effects (Builder pair, set-of-N, etc.). The icon is appended to the
     * detail type so it shows up in the TUI as {@code INVENTOR-A},
     * {@code INVENTOR-B}, ... — display only, never parsed back.
     */
    @Override
    public String cardDetailType() {
        return "INVENTOR-" + iconPerInventor;
    }

    @Override
    public String describe() {
        return "Inventor (icon " + iconPerInventor + "): pair two Inventors with the "
                + "same icon to trigger Inventor-pair rewards from matching Buildings.";
    }
}
