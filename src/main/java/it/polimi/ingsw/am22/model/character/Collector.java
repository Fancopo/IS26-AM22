package it.polimi.ingsw.am22.model.character;


import it.polimi.ingsw.am22.model.Era;

public class Collector extends TribeCharacter implements CharacterEffect {

    public Collector(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.COLLECTOR, null);
        setEffect(this);
    }
}
