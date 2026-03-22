package il.polimi.ingse.event;
import il.polimi.ingse.BuildingEffect;
import il.polimi.ingse.Era;
import il.polimi.ingse.character.CharacterType;

import java.util.ArrayList;
import java.util.List;

public class CavePaintings extends Event implements EventEffect {

    public CavePaintings(Era era, int minPlayers) {
        super(era, minPlayers, EventType.CAVE_PAINTING);
    }

    @Override
    public void applyEvent(List<Player> players, char id) {
        int minArtistsRequired = 0;
        int PPtoLose = -2; // Come da tuo snippet, la penalità è sempre -2
        int PPperArtist = 0;

        // 1. Impostazione delle soglie e dei premi in base all'Era
        if (this.era == Era.I) {
            minArtistsRequired = 1;
            PPperArtist = 1;
        } else if (this.era == Era.II) {
            minArtistsRequired = 2;
            PPperArtist = 2;
        } else if (this.era == Era.III) {
            minArtistsRequired = 3;
            PPperArtist = 3;
        }

        // 2. Risoluzione dell'evento per ogni giocatore
        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // Conta il numero di Artisti
            int artistCount = tribe.countCharacters(CharacterType.ARTIST);

            // Controllo per gli Edifici (c'è un edificio che dà 1 Cibo per ogni Artista durante questo evento)
            int extraFoodPerArtist = 0;
            for (Building building : tribe.getBuildings()) {
                BuildingEffect effect = building.getEffect();
                if (effect instanceof EventYieldBonusEffect) {
                    // Supponiamo che l'edificio aggiunga un bonus in cibo durante gli eventi
                    extraFoodPerArtist += 1;
                }
            }

            // Assegna eventuale cibo bonus degli edifici
            if (extraFoodPerArtist > 0 && artistCount > 0) {
                player.addFood(artistCount * extraFoodPerArtist);
                System.out.println(player.getNickname() + " ottiene " + (artistCount * extraFoodPerArtist) + " cibo extra grazie a un Edificio!");
            }

            // 3. Calcolo e Assegnazione dei Punti Prestigio
            if (artistCount >= minArtistsRequired) {
                // Il giocatore ha abbastanza Artisti: guadagna PP per ogni Artista
                int earnedPP = artistCount * PPperArtist;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " ha " + artistCount +
                        " Artisti (minimo richiesto: " + minArtistsRequired +
                        "). Guadagna " + earnedPP + " PP!");
            } else {
                // Il giocatore non ha abbastanza Artisti: subisce la penalità
                player.addPP(PPtoLose); // PPtoLose è già negativo (-2)
                System.out.println(player.getNickname() + " ha solo " + artistCount +
                        " Artisti (minimo richiesto: " + minArtistsRequired +
                        "). Subisce " + PPtoLose + " PP.");
            }
        }
    }
}

