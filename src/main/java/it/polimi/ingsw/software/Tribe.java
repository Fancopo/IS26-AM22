package it.polimi.ingsw.software;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Tribe {
    private final List<TribeCharacter> members;
    private final List<Building> buildings;

    public Tribe() {
        this.members = new ArrayList<>();
        this.buildings = new ArrayList<>();
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

    public List<TribeCharacter> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public List<Building> getBuildings() {
        return Collections.unmodifiableList(buildings);
    }

    public int getNumberOfMembers() {
        return members.size();
    }

    public int getNumberOfBuildings() {
        return buildings.size();
    }

    public int countCharacter(char type) {
        int count = 0;

        for (TribeCharacter character : members) {
            if (character.getCharacterType() == type) {
                count++;
            }
        }

        return count;
    }

    @Override
    public String toString() {
        return "Tribe{" +
                "members=" + members.size() +
                ", buildings=" + buildings.size() +
                '}';
    }
}
