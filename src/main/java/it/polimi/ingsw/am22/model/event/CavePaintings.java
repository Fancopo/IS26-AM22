package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.Building.Building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.CharacterType;

import java.util.List;

public class CavePaintings extends Event implements EventEffect {

    public CavePaintings(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.CAVE_PAINTING, eventEffect);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int minArtistsRequired = 0;
        int PPtoLose = -2; // Come da tuo snippet, la penalità è sempre -2
        int PPperArtist = 0;

        // 1. Impostazione delle soglie e dei premi in base all'Era
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

        // 2. Risoluzione dell'evento per ogni giocatore
        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // Conta il numero di Artisti
            int artistCount = tribe.countCharacters(CharacterType.ARTIST);

            // Controllo per gli Edifici (c'è un edificio che dà 1 Cibo per ogni Artista durante questo evento)
            for (Building building : player.getTribe().getBuildings()){
                building.getEffect().applyEventBonus(EventType.CAVE_PAINTING,player,artistCount);
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

