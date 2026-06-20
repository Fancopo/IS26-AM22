package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import it.polimi.ingsw.am22.model.event.EventType;

import java.io.Serializable;

/**
 * Strategy describing what a {@link Building} does beyond its base price and
 * victory points. Every hook has a no-op / zero default, so a building with no
 * special behaviour simply inherits them and a concrete effect overrides only
 * the hooks it cares about.
 */
public interface BuildingEffect extends Serializable {
    /**
     * @param tribe the owner's tribe
     * @return the end-game victory points contributed by this effect
     */
    default int calculateEndGame(Tribe tribe) { return 0; }

    /** @return the extra Shaman star icons granted by this effect */
    default int getExtraShamanIcons() {return 0;}

    /** @return {@code true} if this effect prevents the Shaman PP loss */
    default boolean preventsShamanPPLoss() {return false;}

    /** @return {@code true} if this effect doubles the Shaman win PP */
    default boolean doublesShamanWinPP() {return false;}

    /**
     * Hook fired when the owner's totem lands on a turn-order food slot.
     *
     * @param owner the building's owner
     */
    default void onTotemPlaced(Player owner) {}

    /** @return {@code true} if this effect grants an extra buy at round end */
    default boolean hasExtraBuyAtRoundEnd() {return false;}

    /**
     * @param tribe the owner's tribe
     * @return how many extra characters can be fed during the Sustenance event
     */
    default int getSustenanceDiscount(Tribe tribe) { return 0; }

    /**
     * Hook fired when a character is added to the owner's tribe.
     *
     * @param player  the building's owner
     * @param newChar the character just added
     */
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}

    /**
     * Hook fired while an event resolves, to apply a per-event bonus.
     *
     * @param eventType      the event being resolved
     * @param player         the building's owner
     * @param characterCount the relevant character count for the bonus
     */
    default void applyEventBonus(EventType eventType, Player player, int characterCount) {}

    /**
     * Human-readable effect text for the TUI {@code check} command. Default is
     * empty so a Building with no effect shows only its base price/PP info.
     *
     * @return the effect description, or an empty string if there is none
     */
    default String describe() { return ""; }
}
