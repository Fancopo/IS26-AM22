package it.polimi.ingsw.am22.character;


import com.fasterxml.jackson.annotation.JsonCreator;
import it.polimi.ingsw.am22.*;
import it.polimi.ingsw.am22.Building.Building;

public class TribeCharacter extends Card {

    private CharacterType characterType;
    private CharacterEffect effect;

    @JsonCreator
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

    public void addToTribe(Player player, Tribe tribe) {
        tribe.getMembers().add(this);

        if (effect != null) {
            effect.applyImmediateEffect(player, tribe);
        }

        for (Building b : tribe.getBuildings()) {
            if (b.getEffect() != null) {
                b.getEffect().onCharacterAdded(player, this);
            }
        }
    }

    public int getNumStars() {
        return effect != null ? effect.getNumStars() : 0;
    }

    public char getIconPerInventor() {
        return effect != null ? effect.getIconPerInventor() : '0';
    }

    public int getDiscountFood() {
        return effect != null ? effect.getDiscountFood() : 0;
    }

    public int getPP() {
        return effect != null ? effect.getPP() : 0;
    }
}