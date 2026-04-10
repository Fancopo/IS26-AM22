package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;

import static it.polimi.ingsw.am22.Building.Building.FinalBuildingPP;

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
            throw new IllegalArgumentException("L'importo da pagare non può essere negativo.");
        }

        if (this.food < amount) {
            throw new IllegalStateException("Cibo insufficiente.");
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

        // Costruttori: somma dei PP stampati sulle carte
        for (TribeCharacter c : tribe.getMembers()) {
            if (c.getCharacterType() == CharacterType.BUILDER) {
                points += c.getPP();
            }
        }

        // Inventori: numero inventori * numero icone invenzione diverse
        int uniqueInventorIcons = tribe.countUniqueInventorIcons();
        points += inventors * uniqueInventorIcons;

        // Artisti: 10 PP ogni 2 artisti
        points += (artists / 2) * 10;

        return points;
    }


    public int finalPP() {
        int total = this.PP; // PP accumulati durante la partita

        // 1) PP dei personaggi
        total += calculateCharacterEndgamePP();

        // 2) PP degli edifici
        total += FinalBuildingPP(this.tribe);

        return total;
    }

    public boolean hasExtraBuyAtRoundEnd() {
        return this.tribe != null && this.tribe.hasExtraBuyAtRoundEnd();
    }

}