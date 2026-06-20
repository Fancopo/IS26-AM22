package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;

/**
 * A character card belonging to a player's tribe. Concrete subclasses (Hunter,
 * Builder, Inventor, …) register themselves as their own {@link CharacterEffect}
 * and override the relevant hooks; this base class provides the default
 * (no-effect) attribute values so callers never need to know the concrete type.
 */
public class TribeCharacter extends Card {

    private CharacterType characterType;
    private CharacterEffect effect;

    /**
     * @param id            the card id
     * @param era           the Era the card belongs to
     * @param minPlayers    the minimum player count for this card to be in play
     * @param characterType the character type
     * @param effect        the character effect (subclasses pass {@code null} and
     *                       register themselves via {@link #setEffect})
     */
    public TribeCharacter(String id, Era era, int minPlayers, CharacterType characterType, CharacterEffect effect) {
        super(id, era, minPlayers);
        this.characterType = characterType;
        this.effect = effect;
    }

    /** @return the character type */
    public CharacterType getCharacterType() {
        return characterType;
    }

    /** @return this character's effect */
    public CharacterEffect getEffect() {
        return effect;
    }

    /**
     * Lets subclasses register themselves as their own effect after the super() call.
     *
     * @param effect the effect to use
     */
    protected void setEffect(CharacterEffect effect) {
        this.effect = effect;
    }

    /**
     * Adds this character to the tribe, letting the owner's buildings react and
     * then running the subclass's {@link #onAddedToTribe(Player)} hook.
     *
     * @param player the owner of the tribe
     * @param tribe  the tribe to join
     */
    public void addToTribe(Player player, Tribe tribe) {
        tribe.addCharacter(this);
        for (Building b : tribe.getBuildings()) {
            if (b.getEffect() != null) {
                b.getEffect().onCharacterAdded(player, this);
            }
        }
        onAddedToTribe(player);
    }

    /**
     * Hook invoked after the character has joined the tribe. Subclasses override
     * to apply immediate per-character effects (e.g. the Hunter food bonus).
     *
     * @param player the owner of the tribe
     */
    protected void onAddedToTribe(Player player) { }

    @Override
    public String cardCategory() { return "CHARACTER"; }

    @Override
    public String cardDetailType() { return String.valueOf(characterType); }

    /** @return the Inventor icon; {@code '0'} for non-Inventors */
    public char getIconPerInventor(){return '0';}

    /** @return the Builder food discount; 0 for non-Builders */
    public int getDiscountFood(){
        return 0;
    }

    /** @return the printed end-game victory points; 0 unless the card prints any */
    public int getPP(){
        return 0;
    }

}
