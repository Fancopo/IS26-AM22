package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

/**
 * Cave Paintings event. Each player with at least the Era's required number of
 * Artists scores PP per Artist; players below the threshold suffer a flat
 * penalty. Buildings may add a per-Artist bonus.
 */
public class CavePaintings extends Event implements EventEffect {

    private static final int PENALTY_PP = -2;

    /**
     * @param id          the card id
     * @param era         the Era the card belongs to
     * @param minPlayers  the minimum player count for this card to be in play
     * @param eventEffect ignored; the card registers itself as its own effect
     */
    public CavePaintings(String id, Era era, int minPlayers, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.CAVE_PAINTING, eventEffect);
        setEffect(this);
    }

    /**
     * Scores Artists for every player (Era-dependent threshold and reward), then
     * applies any Building per-Artist bonus.
     *
     * @param players the players in the game
     * @param id      the id of the resolving event
     */
    @Override
    public void applyEvent(List<Player> players, String id) {
        int minArtistsRequired = switch (getEra()) {
            case I -> 1;
            case II -> 2;
            case III -> 3;
        };
        int ppPerArtist = switch (getEra()) {
            case I -> 1;
            case II -> 2;
            case III -> 3;
        };

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int artistCount = tribe.countCharacters(CharacterType.ARTIST);

            for (Building building : tribe.getBuildings()) {
                building.getEffect().applyEventBonus(EventType.CAVE_PAINTING, player, artistCount);
            }

            if (artistCount >= minArtistsRequired) {
                player.addPP(artistCount * ppPerArtist);
            } else {
                player.addPP(PENALTY_PP);
            }
        }
    }

    @Override
    public String describe() {
        int min = switch (getEra()) { case I -> 1; case II -> 2; case III -> 3; };
        int pp  = switch (getEra()) { case I -> 1; case II -> 2; case III -> 3; };
        return "Cave Paintings event (Era " + getEra() + "): each player with at "
                + "least " + min + " Artist(s) gains " + pp + " PP per Artist; "
                + "anyone below that threshold suffers a " + PENALTY_PP + " PP penalty. "
                + "Events cannot be picked into the tribe.";
    }
}
