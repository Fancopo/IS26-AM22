package it.polimi.ingsw.am22;

public abstract class Card {
    private Era era;
    private int minPlayers;

    public Card(Era era, int minPlayers) {
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public Era getEra() { return era; }
    public int getMinPlayers() { return minPlayers; }
}