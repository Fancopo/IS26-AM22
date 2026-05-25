package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class CavePaintings extends Event implements EventEffect {

    private static final int PENALTY_PP = -2;

    public CavePaintings(String id, Era era, int minPlayers, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.CAVE_PAINTING, eventEffect);
        setEffect(this);
    }

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
