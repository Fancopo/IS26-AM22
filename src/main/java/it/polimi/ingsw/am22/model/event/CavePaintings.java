package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.building.Building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class CavePaintings extends Event implements EventEffect {

    public CavePaintings(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.CAVE_PAINTING, eventEffect);
        setEffect(this);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int minArtistsRequired = 0;
        int PPtoLose = -2; // Penalty is always -2
        int PPperArtist = 0;

        // 1. Set thresholds and rewards based on the Era
        if (this.getEra() == Era.I) {
            minArtistsRequired = 1;
            PPperArtist = 1;
        } else if (this.getEra() == Era.II) {
            minArtistsRequired = 2;
            PPperArtist = 2;
        } else if (this.getEra() == Era.III) {
            minArtistsRequired = 3;
            PPperArtist = 3;
        }

        // 2. Resolve the event for each player
        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // Count Artists
            int artistCount = tribe.countCharacters(CharacterType.ARTIST);

            // Building bonus check (one building grants 1 Food per Artist during this event)
            for (Building building : player.getTribe().getBuildings()){
                building.getEffect().applyEventBonus(EventType.CAVE_PAINTING,player,artistCount);
            }

            // 3. Compute and assign Prestige Points
            if (artistCount >= minArtistsRequired) {
                // Player has enough Artists: gains PP per Artist
                int earnedPP = artistCount * PPperArtist;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " has " + artistCount +
                        " Artists (minimum required: " + minArtistsRequired +
                        "). Gains " + earnedPP + " PP!");
            } else {
                // Not enough Artists: takes the penalty
                player.addPP(PPtoLose); // PPtoLose is already negative (-2)
                System.out.println(player.getNickname() + " only has " + artistCount +
                        " Artists (minimum required: " + minArtistsRequired +
                        "). Loses " + PPtoLose + " PP.");
            }
        }
    }
}


