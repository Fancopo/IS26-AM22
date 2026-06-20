package it.polimi.ingsw.am22.model.character;


import it.polimi.ingsw.am22.model.Era;

/**
 * Collector character. Each Collector feeds three characters for free during the
 * Sustenance event, reducing the food the owner must pay.
 */
public class Collector extends TribeCharacter implements CharacterEffect {

    /**
     * @param id         the card id
     * @param era        the Era the card belongs to
     * @param minPlayers the minimum player count for this card to be in play
     */
    public Collector(String id, Era era, int minPlayers) {
        super(id, era, minPlayers, CharacterType.COLLECTOR, null);
        setEffect(this);
    }

    @Override
    public String describe() {
        return "Collector: each Collector in your tribe feeds 3 characters during "
                + "the Sustenance event (reducing the food you have to pay).";
    }
}
