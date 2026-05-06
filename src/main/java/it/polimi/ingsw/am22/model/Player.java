package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;

import static it.polimi.ingsw.am22.model.building.Building.FinalBuildingPP;

public class Player {
    private String nickname;
    private int PP;
    private int food;
    private Tribe tribe;
    private Totem totem;

    public Player(String nickname) {
        this.nickname = nickname;
        this.PP = 0;
        this.food = 0;
        this.tribe = new Tribe();
        this.totem = null;
    }

    public String getNickname() {
        return nickname;
    }

    public int getPP() {
        return PP;
    }

    public int getFood() {
        return food;
    }

    public Tribe getTribe() {
        return tribe;
    }

    public Totem getTotem() {
        return totem;
    }

    public void setTotem(Totem totem) {
        this.totem = totem;
    }

    public void addFood(int amount) {
        this.food += amount;
    }

    public void payFood(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount to pay cannot be negative.");
        }

        if (this.food < amount) {
            throw new IllegalStateException("Insufficient food.");
        }

        this.food -= amount;
    }

    /**
     * Adds or subtracts prestige points.
     */
    public void addPP(int amount) {
        this.PP += amount;
    }

    private int calculateCharacterEndgamePP() {
        int points = 0;

        int builders = tribe.countCharacters(CharacterType.BUILDER);
        int inventors = tribe.countCharacters(CharacterType.INVENTOR);
        int artists = tribe.countCharacters(CharacterType.ARTIST);

        // Builders: sum of PP printed on cards
        for (TribeCharacter c : tribe.getMembers()) {
            if (c.getCharacterType() == CharacterType.BUILDER) {
                points += c.getPP();
            }
        }

        // Inventors: number of inventors * number of distinct invention icons
        int uniqueInventorIcons = tribe.countUniqueInventorIcons();
        points += inventors * uniqueInventorIcons;

        // Artists: 10 PP for every 2 artists
        points += (artists / 2) * 10;

        return points;
    }


    public int finalPP() {
        int total = this.PP; // PP accumulated during the match

        // 1) Character PP
        total += calculateCharacterEndgamePP();

        // 2) Building PP
        total += FinalBuildingPP(this.tribe);

        return total;
    }

    public boolean hasExtraBuyAtRoundEnd() {
        return this.tribe != null && this.tribe.hasExtraBuyAtRoundEnd();
    }

}
