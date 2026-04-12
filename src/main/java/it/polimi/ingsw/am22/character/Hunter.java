package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;


public class Hunter extends TribeCharacter implements CharacterEffect{
    private final boolean HasFoodIcon;

    public Hunter(String id, Era era, int minPlayers, boolean hasFoodIcon) {
        super(id, era, minPlayers, CharacterType.HUNTER, null);
        this.HasFoodIcon = hasFoodIcon;
        setEffect(this);
    }

    public boolean hasFoodIcon() {
        return HasFoodIcon;
    }

@Override
    public void applyImmediateEffect(Player player, Tribe tribe){

        if(this.HasFoodIcon){
            int foodToAdd = player.getTribe().countCharacters(getCharacterType());
            player.addFood(foodToAdd);
            System.out.println("Cacciatore con icona giocato! Aggiunto " +foodToAdd + " cibo.");
        }
    }

}
