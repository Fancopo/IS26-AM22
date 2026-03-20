package it.polimi.ingsw.am22;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public enum CharacterType {
    INVENTOR, BUILDER, GATHERER, SHAMAN, ARTIST, HUNTER
}

// Stub for the Player
class Player {
    private int food;
    private int prestigePoints;
    private Tribe tribe;

    public void addFood(int amount) { this.food += amount; }
    public void addPP(int amount) { this.prestigePoints += amount; }
    public Tribe getTribe() { return tribe; }

    // Flags for event modifiers granted by buildings
    public boolean hasShamanicProtection = false;
    public boolean hasShamanDoublePP = false;
    public boolean canTakeExtraCardEndOfRound = false;
}

// Stub for the Tribe
class Tribe {
    private List<TribeCharacter> members = new ArrayList<>();
    public List<TribeCharacter> getMembers() { return members; }

    // Helper to count unique character types
    public int getUniqueCharacterTypeCount() {
        Set<CharacterType> uniqueTypes = new HashSet<>();
        for (TribeCharacter c : members) {
            uniqueTypes.add(c.getCharacterType());
        }
        return uniqueTypes.size();
    }
}

// Stub for the Tribe Character
abstract class TribeCharacter extends Card {
    private CharacterType characterType;
    public TribeCharacter(char id, Era era, int minPlayers, CharacterType type) {
        super(id, "Character", era, minPlayers);
        this.characterType = type;
    }
    public CharacterType getCharacterType() { return characterType; }
    public String getSubIcon() { return ""; } // E.g., for specific Inventor icons
}