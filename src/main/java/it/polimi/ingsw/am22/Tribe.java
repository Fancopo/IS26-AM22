package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;

import java.util.*;

public class Tribe {
    private List<TribeCharacter> members;
    private List<Building> buildings;

    public Tribe() {
        this.members = new ArrayList<>();
        this.buildings = new ArrayList<>();
    }

    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("Card cannot be null.");
        }
        card.addToTribe(this);
    }

    public void addCharacter(TribeCharacter character) {
        if (character == null) {
            throw new IllegalArgumentException("Character cannot be null.");
        }
        members.add(character);
    }

    public void addBuilding(Building building) {
        if (building == null) {
            throw new IllegalArgumentException("Building cannot be null.");
        }
        buildings.add(building);
    }

    /**
     * Counts the characters of a given type.
     * This method can also support end-game rules
     * such as artists, builders, inventors, etc.
     */
    public int countCharacters(CharacterType type) {
        int count = 0;

        for (TribeCharacter character : members) {
            if (character.getCharacterType() == type) {
                count++;
            }
        }

        return count;
    }

    public int countUniqueInventorIcons() {
        Set<Character> uniqueIcons = new HashSet<>();

        for (TribeCharacter character : members) {
            if (character.getCharacterType() == CharacterType.INVENTOR) {
                uniqueIcons.add(character.getIconPerInventor());
            }
        }

        return uniqueIcons.size();
    }

    public int getBuilderDiscount() {
        int discount = 0;

        for (TribeCharacter character : members) {
            if (character.getCharacterType() == CharacterType.BUILDER) {
                discount += character.getDiscountFood();
            }
        }
        return discount;
    }

    public boolean hasExtraBuyAtRoundEnd() {
        for (Building b : buildings) {
            if (b.grantsExtraBuyAtRoundEnd()) {
                return true;
            }
        }
        return false;
    }

    public List<TribeCharacter> getMembers() {
        return List.copyOf(members);
    }

    public List<Building> getBuildings() {
        return List.copyOf(buildings);
    }
}
