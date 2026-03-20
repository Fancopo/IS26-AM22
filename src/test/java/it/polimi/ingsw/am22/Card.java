package it.polimi.ingsw.am22;

public abstract class Card {
    private char id;
    private String type;
    private Era era;
    private int minPlayers;

    public Card(char id, String type, Era era, int minPlayers) {
        this.id = id;
        this.type = type;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public char getId() {
        return id;
    }

    public Era getEra() {
        return era;
    }

    public String getType() {
        return type;
    }

    public int getMinPlayers() {
        return minPlayers;
    }
}