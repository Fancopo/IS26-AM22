package it.polimi.ingsw.am22.character;


import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;

public class Collector extends TribeCharacter implements CharacterEffect{

    public Collector(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.COLLECTOR, null);
        setEffect(this);
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

}
