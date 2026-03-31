package am22;

import il.polimi.ingse.Era

public abstract class Card {
    private String id;
    private Era era;
    private int minPlayers;

    public Card(String id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }
    public abstract void accept(CardVisitor visitor);

    public String getId() { return id; }
    public Era getEra() { return era; }
    public int getMinPlayers() { return minPlayers; }