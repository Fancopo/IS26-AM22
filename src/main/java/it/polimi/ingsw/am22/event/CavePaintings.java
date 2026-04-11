package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;

import java.util.List;

public class CavePaintings implements EventEffect {

    private Era era;

    public CavePaintings(Era era) {
        this.era = era;
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int minArtistsRequired = 0;
        int PPtoLose = -2;
        int PPperArtist = 0;

        if (era == Era.I) {
            minArtistsRequired = 1;
            PPperArtist = 1;
        } else if (era == Era.II) {
            minArtistsRequired = 2;
            PPperArtist = 2;
        } else if (era == Era.III) {
            minArtistsRequired = 3;
            PPperArtist = 3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            int artistCount = tribe.countCharacters(CharacterType.ARTIST);

            for (Building building : player.getTribe().getBuildings()) {
                building.getEffect().applyEventBonus(EventType.CAVE_PAINTING, player, artistCount);
            }

            if (artistCount > 0) {
                player.addFood(artistCount);
                System.out.println(player.getNickname() + " ottiene " + artistCount);
            }

            if (artistCount >= minArtistsRequired) {
                int earnedPP = artistCount * PPperArtist;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " ha " + artistCount +
                        " Artisti (minimo richiesto: " + minArtistsRequired +
                        "). Guadagna " + earnedPP + " PP!");
            } else {
                player.addPP(PPtoLose);
                System.out.println(player.getNickname() + " ha solo " + artistCount +
                        " Artisti (minimo richiesto: " + minArtistsRequired +
                        "). Subisce " + PPtoLose + " PP.");
            }
        }
    }
}