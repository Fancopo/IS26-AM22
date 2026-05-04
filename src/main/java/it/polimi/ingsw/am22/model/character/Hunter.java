package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;


public class Hunter extends TribeCharacter implements CharacterEffect {
    private final boolean hasFoodIcon;

    public Hunter(String id, Era era, int minPlayers, boolean hasFoodIcon) {
        super(id, era, minPlayers, CharacterType.HUNTER, null);
        this.hasFoodIcon = hasFoodIcon;
        setEffect(this);
    }

    public boolean hasFoodIcon() {
        return hasFoodIcon;
    }

    @Override
    protected void onAddedToTribe(Player player) {
        if (hasFoodIcon) {
            int foodToAdd = player.getTribe().countCharacters(getCharacterType());
            player.addFood(foodToAdd);
        }
    }
}
