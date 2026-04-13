package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Building.EventYieldBonusEffect;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.Artist;
import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class CavePaintingsTest {

    @Test
    void testCavePaintingsEra1_NoArtists_AndNullTribe() {
        CavePaintings event = new CavePaintings("cp_01", Era.I, 3, EventType.CAVE_PAINTING, null);

        Player normalPlayer = new Player("Christian");
        // Non aggiungiamo Artisti, quindi artistCount = 0

        // Creiamo un giocatore fittizio con tribù nulla per coprire la riga "if (tribe == null) continue;"
        Player nullTribePlayer = new Player("Fantasma") {
            @Override
            public Tribe getTribe() { return null; }
        };

        assertDoesNotThrow(() -> event.applyEvent(Arrays.asList(normalPlayer, nullTribePlayer), "cp_01"));
        // normalPlayer subisce -2 PP, 0 cibo. nullTribePlayer viene saltato.
        assertEquals(0, normalPlayer.getFood(), "Il cibo non deve essere stato speso grazie allo sconto");
        assertEquals(-2, normalPlayer.getPP(), "normalPlayer subisce -2 PP");
    }

    @Test
    void testCavePaintingsEra2_NotEnoughArtists() {
        CavePaintings event = new CavePaintings("cp_02", Era.II, 3, EventType.CAVE_PAINTING, null);
        Player player = new Player("Christian");

        // Aggiungiamo 1 Artista. (L'Era II ne richiede minimo 2!)
        player.getTribe().addCharacter(new Artist("art_01", Era.I, 3));

        assertDoesNotThrow(() -> event.applyEvent(List.of(player), "cp_02"));
        //finisce nell'else dei PP subendo la penalità.
        assertEquals(-2, player.getPP(), "normalPlayer subisce -2 PP");
    }

    @Test
    void testCavePaintingsEra3_NotEnoughArtists_WithBuildings() {
        CavePaintings event = new CavePaintings("cp_03", Era.III, 3, EventType.CAVE_PAINTING, null);
        Player player = new Player("Christian");

        // Aggiungiamo 2 Artisti (L'Era III ne richiede minimo 3)
        player.getTribe().addCharacter(new Artist("art_01", Era.I, 3));
        player.getTribe().addCharacter(new Artist("art_02", Era.I, 3));

        // Aggiungiamo un edificio con effetto per coprire il ciclo for degli edifici
        EventYieldBonusEffect dummyEffect = new EventYieldBonusEffect(EventType.CAVE_PAINTING, 1, 0);
            // (Aggiungi qui eventuali altri metodi obbligatori dell'interfaccia BuildingEffect)
        // Adatta i parametri di new Building in base al tuo costruttore reale
        Building building = new Building("b_01", Era.I, 3, 2, 5, dummyEffect);
        player.getTribe().addBuilding(building);
        assertEquals(0, player.getPP(), "Il giocatore deve partire da 0 PP");

        assertDoesNotThrow(() -> event.applyEvent(List.of(player), "cp_03"));
        assertEquals(2, player.getFood(), "Il cibo non deve essere stato speso grazie allo sconto");
        assertEquals(-2, player.getPP(), "normalPlayer subisce -2 PP");
    }


    @Test
    void testCavePaintingsEra3_EnoughArtists_WithBuildings() {
        CavePaintings event = new CavePaintings("cp_03", Era.III, 3, EventType.CAVE_PAINTING, null);
        Player player = new Player("Christian");

        // Aggiungiamo 3 Artisti (L'Era III ne richiede minimo 3)
        player.getTribe().addCharacter(new Artist("art_01", Era.I, 3));
        player.getTribe().addCharacter(new Artist("art_02", Era.I, 3));
        player.getTribe().addCharacter(new Artist("art_03", Era.I, 3));

        // Aggiungiamo un edificio con effetto per coprire il ciclo for degli edifici
        EventYieldBonusEffect dummyEffect = new EventYieldBonusEffect(EventType.CAVE_PAINTING, 1, 0);
        // Adatta i parametri di new Building in base al tuo costruttore reale
        Building building = new Building("b_01", Era.I, 3, 2, 5, dummyEffect);
        player.getTribe().addBuilding(building);

        assertDoesNotThrow(() -> event.applyEvent(List.of(player), "cp_03"));
        // Il giocatore ha abbastanza artisti: guadagna 9 PP (3 * 3) e prende 3 cibo.
        assertEquals(3, player.getFood(), "Il cibo non deve essere stato speso grazie allo sconto");
        assertEquals(9, player.getPP(), "normalPlayer subisce -2 PP");
    }
}
