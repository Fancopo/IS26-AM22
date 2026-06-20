package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;

import java.io.Serializable;
import java.util.*;

/**
 * A player's tribe: the {@link TribeCharacter}s and {@link Building}s they have
 * acquired. Besides storing the cards, it answers the aggregate queries the
 * rules need (Builder discount, character counts, end-of-round extra buy).
 */
public class Tribe implements Serializable {
    private List<TribeCharacter> members;
    private List<Building> buildings;

    /** Creates an empty tribe. */
    public Tribe() {
        this.members = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }

    /**
     * Adds a card to the tribe, delegating to the card's own placement logic
     * (which routes it to the characters or the buildings).
     *
     * @param player the owner of the tribe
     * @param card   the card to add
     * @throws IllegalArgumentException if {@code card} is null
     */
    public void addCard(Player player, Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null.");
        }
        card.addToTribe(player, this);
    }

    /**
     * Adds a character to the tribe.
     *
     * @param character the character to add
     * @throws IllegalArgumentException if {@code character} is null
     */
    public void addCharacter(TribeCharacter character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null.");
        }
        members.add(character);
    }

    /**
     * Adds a building to the tribe.
     *
     * @param building the building to add
     * @throws IllegalArgumentException if {@code building} is null
     */
    public void addBuilding(Building building) {
        if (building == null) {
            throw new IllegalArgumentException("Building cannot be null.");
        }
        buildings.add(building);
    }

    /**
     * Counts the characters of a given type in the tribe.
     *
     * @param type the character type to count
     * @return the number of members of that type
     */
    public int countCharacters(CharacterType type) {
        int count = 0;
        for (TribeCharacter character : members) {
            if (character.getCharacterType() == type) count++;
        }
        return count;
    }

    /** @return the number of distinct invention icons among the tribe's Inventors */
    public int countUniqueInventorIcons() {
        Set<Character> icons = new HashSet<>();
        for (TribeCharacter character : members) {
            if (character.getCharacterType() == CharacterType.INVENTOR) {
                icons.add(character.getIconPerInventor());
            }
        }
        return icons.size();
    }

    /** @return the total Builder food discount provided by the tribe */
    public int getBuilderDiscount() {
        int discount = 0;
        for (TribeCharacter character : members) {
            if (character.getCharacterType() == CharacterType.BUILDER) {
                discount += character.getDiscountFood();
            }
        }
        return discount;
    }

    /** @return {@code true} if any building grants an extra buy at round end */
    public boolean hasExtraBuyAtRoundEnd() {
        for (Building b : buildings) {
            if (b.grantsExtraBuyAtRoundEnd()) return true;
        }
        return false;
    }

    /** @return an unmodifiable view of the tribe's characters */
    public List<TribeCharacter> getMembers() {
        return List.copyOf(members);
    }

    /** @return an unmodifiable view of the tribe's buildings */
    public List<Building> getBuildings() {
        return List.copyOf(buildings);
    }
}
