package il.polimi.ingse.event;
import java.util.ArrayList;
import java.util.List;

public class CavePaintings extends Event implements EventEffect {

    @Override
    public void applyEvent(List<Player> players, char id) {
        // Recuperiamo i parametri dell'evento in base all'ID (Era I, II o III) per era 3 0-2 artisti(-2) 3+ artisti(3 per artisti) per era 2 0-1 artisti(-2) 2+ artisti(2 per artisti) per era 1 0 artisti （-2） 1+artisti (1 per artisti)
        int threshold = getThreshold(id);           // Es: 2 (Artisti minimi richiesti)
        int ppToLose = getLossAmount(id);           // Es: 3 (PP da sottrarre se si fallisce)
        int ppPerArtistToWin = getWinMultiplier(id);// Es: 2 (PP vinti PER ARTISTA se si supera la soglia)


        for (Player player : players) {
            //  il metodo per contare direttamente gli artisti!
            int numArtists = player.getTribe().countCharacter("Artist");

            // Controllo dell'edificio bonus per l'evento Pitture Rupestri
            boolean hasFoodForArtistsBuilding = false;
            for (Building building : player.getTribe().getBuildings()) {
                if ("FoodDuringCavePaintings".equals(building.getType())) {
                    hasFoodForArtistsBuilding = true;
                    break;
                }
            }

            // 2. Risoluzione dei Punti Prestigio
            if (numArtists < threshold) {
                // Sotto la soglia (Riga superiore): penalità fissa
                player.addPP(-ppToLose);
            } else {
                // Pari o sopra la soglia (Riga inferiore): premio x numero di Artisti
                int pointsEarned = numArtists * ppPerArtistToWin;
                player.addPP(pointsEarned);
            }

            // 3. Applicazione del potere dell'Edificio (se posseduto)
            // "Prendete 1 Cibo per ogni Artista nella vostra tribù"
            if (hasFoodForArtistsBuilding && numArtists > 0) {
                player.addFood(numArtists);
            }


        }
    }
}

