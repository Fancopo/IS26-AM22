package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.Hunter;
import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.Building.BuildingEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

class huntingTest {

    @Test
    void testHuntingEra1_NoHunters_AndNullTribe() {
        Hunting event = new Hunting("hunt_01", Era.I, 3, EventType.HUNTING, null);

        Player normalPlayer = new Player("Christian");
        // Non aggiungiamo Cacciatori. Si aspetta 0 Cibo e 0 PP.

        // Giocatore fittizio per coprire la riga "if (tribe == null) continue;"
        Player nullTribePlayer = new Player("Fantasma") {
            @Override
            public Tribe getTribe() { return null; }
        };

        assertDoesNotThrow(() -> event.applyEvent(Arrays.asList(normalPlayer, nullTribePlayer), "hunt_01"));

        // VERIFICA: Nessun cacciatore = Nessuna risorsa
        assertEquals(0, normalPlayer.getFood(), "Non avendo cacciatori, il cibo deve restare invariato");
        assertEquals(0, normalPlayer.getPP(), "Non avendo cacciatori, i PP devono restare invariati");
    }

    @Test
    void testHuntingEra2_WithHunters_AndBuildings() {
        Hunting event = new Hunting("hunt_01", Era.II, 3, EventType.HUNTING, null);
        Player player = new Player("Christian");

        // Aggiungiamo 2 Cacciatori
        player.getTribe().addCharacter(new Hunter("h1", Era.I, 3, false));
        player.getTribe().addCharacter(new Hunter("h2", Era.I, 3, true));

        // Aggiungiamo un Edificio per testare il ciclo 'for' interno in sicurezza
        BuildingEffect dummyEffect = new BuildingEffect() {
            @Override
            public void applyEventBonus(EventType type, Player p, int count) {}
        };
        // Usa i parametri corretti per il tuo costruttore Building
        Building building = new Building("b_01", Era.I, 3, 2, 5, dummyEffect);
        player.getTribe().addBuilding(building);

        assertDoesNotThrow(() -> event.applyEvent(List.of(player), "hunt_02"));

        // VERIFICA ERA II: 2 Cacciatori. Cibo = 1x2=2. PP = 2x2=4.
        assertEquals(2, player.getFood(), "Dovrebbe aver ottenuto 2 Cibo (1 per ogni Cacciatore)");
        assertEquals(4, player.getPP(), "Dovrebbe aver ottenuto 4 PP (2 per ogni Cacciatore in Era II)");
    }

    @Test
    void testHuntingEra3_CalculatesPPCorrectly() {
        Hunting event = new Hunting("hunt_01", Era.III, 3, EventType.HUNTING, null);
        Player player = new Player("Christian");

        // Aggiungiamo 3 Cacciatori
        player.getTribe().addCharacter(new Hunter("h1", Era.I, 3, false));
        player.getTribe().addCharacter(new Hunter("h2", Era.I, 3, false));
        player.getTribe().addCharacter(new Hunter("h3", Era.I, 3, false));

        assertDoesNotThrow(() -> event.applyEvent(List.of(player), "hunt_03"));

        // VERIFICA ERA III: 3 Cacciatori. Cibo = 1x3=3. PP = 3x3=9.
        assertEquals(3, player.getFood(), "Dovrebbe aver ottenuto 3 Cibo");
        assertEquals(9, player.getPP(), "Dovrebbe aver ottenuto 9 PP (3 per ogni Cacciatore in Era III)");
    }
}