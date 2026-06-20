package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;

import java.io.Serializable;

import static it.polimi.ingsw.am22.model.building.Building.FinalBuildingPP;

/**
 * A participant in the match: their nickname, victory points (PP) and food, the
 * {@link Tribe} they build up and the {@link Totem} they move around the board.
 */
public class Player implements Serializable {
    private String nickname;
    private int PP;
    private int food;
    private Tribe tribe;
    private Totem totem;

    /**
     * Creates a player with zero PP and food, an empty tribe and no totem yet.
     *
     * @param nickname the player's nickname
     */
    public Player(String nickname) {
        this.nickname = nickname;
        this.PP = 0;
        this.food = 0;
        this.tribe = new Tribe();
        this.totem = null;
    }

    /** @return the player's nickname */
    public String getNickname() {
        return nickname;
    }

    /** @return the player's current victory points */
    public int getPP() {
        return PP;
    }

    /** @return the player's current food */
    public int getFood() {
        return food;
    }

    /** @return the player's tribe */
    public Tribe getTribe() {
        return tribe;
    }

    /** @return the player's totem, or {@code null} if not assigned yet */
    public Totem getTotem() {
        return totem;
    }

    /**
     * @param totem the totem to assign to this player
     */
    public void setTotem(Totem totem) {
        this.totem = totem;
    }

    /**
     * Adds food, or removes it when {@code amount} is negative.
     *
     * @param amount the food delta
     */
    public void addFood(int amount) {
        this.food += amount;
    }

    /**
     * Pays food from the player's stock.
     *
     * @param amount the food to pay (must be non-negative)
     * @throws IllegalArgumentException if {@code amount} is negative
     * @throws IllegalStateException    if the player has less food than {@code amount}
     */
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
     * Adds victory points, or removes them when {@code amount} is negative.
     *
     * @param amount the PP delta
     */
    public void addPP(int amount) {
        this.PP += amount;
    }

    // End-game points coming from the tribe's characters (Builders, Inventors, Artists).
    private int calculateCharacterEndgamePP() {
        int points = 0;

        // Builders: sum of PP printed on each Builder card.
        for (TribeCharacter c : tribe.getMembers()) {
            if (c.getCharacterType() == CharacterType.BUILDER) {
                points += c.getPP();
            }
        }

        // Inventors: number of inventors * number of distinct invention icons.
        int inventors = tribe.countCharacters(CharacterType.INVENTOR);
        points += inventors * tribe.countUniqueInventorIcons();

        // Artists: 10 PP for every 2 artists.
        int artists = tribe.countCharacters(CharacterType.ARTIST);
        points += (artists / 2) * 10;

        return points;
    }

    /**
     * @return the player's final score: base PP plus the end-game points from
     *         characters and buildings
     */
    public int finalPP() {
        return PP + calculateCharacterEndgamePP() + FinalBuildingPP(tribe);
    }

    /** @return {@code true} if the player owns a building granting an extra buy at round end */
    public boolean hasExtraBuyAtRoundEnd() {
        return this.tribe != null && this.tribe.hasExtraBuyAtRoundEnd();
    }

}
