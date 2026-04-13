package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.*;
import it.polimi.ingsw.am22.Building.Building;

public class TribeCharacter extends Card {

    private CharacterType characterType;
    private CharacterEffect effect;

    public TribeCharacter(String id, Era era, int minPlayers, CharacterType characterType, CharacterEffect effect) {
        super(id, era, minPlayers);
        this.characterType = characterType;
        this.effect = effect;
    }

    public CharacterType getCharacterType() {
        return characterType;
    }

    public CharacterEffect getEffect() {
        return effect;
    }

    protected void setEffect(CharacterEffect effect) {
        this.effect = effect;
    }

    public void addToTribe(Player player, Tribe tribe) {
        tribe.addCharacter(this);
        for (Building b : tribe.getBuildings()) {
            if (b.getEffect() != null) {
                b.getEffect().onCharacterAdded(player, this);
            }
        }
    }
    public int getNumStars(){return 0;}

    public char getIconPerInventor(){return '0';}
    public int getDiscountFood(){
        return 0;
    }public int getPP(){
        return 0;
    }

}