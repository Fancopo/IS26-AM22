package it.polimi.ingsw.am22.model.event;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.character.Shaman;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.ArrayList;

class ShamanicRitualTest {

    // Classe di supporto per non dover riscrivere tutti i metodi di BuildingEffect ogni volta
    abstract class BaseDummyEffect implements BuildingEffect {
        @Override public void applyEventBonus(EventType type, Player p, int count) {}
        // Aggiungi qui eventuali altri metodi base della tua interfaccia con return di default (0 o false)
        public int getExtraShamanIcons() { return 0; }
        public boolean preventsShamanPPLoss() { return false; }
        public boolean doublesShamanWinPP() { return false; }
    }

    @Test
    void testShamanicRitualEra1_NormalWinAndLoss_WithNullTribe() {
        ShamanicRitual event = new ShamanicRitual("sha_01", Era.I, 3, EventType.SHAMANIC_RITUAL, null);

        Player winner = new Player("Vincitore");
        winner.getTribe().addCharacter(new Shaman("s1", Era.I, 3, 3)); // 3 icone

        Player loser = new Player("Perdente");
        loser.getTribe().addCharacter(new Shaman("s2", Era.I, 3, 1)); // 1 icona

        Player nullTribePlayer = new Player("Fantasma") {
            @Override public Tribe getTribe() { return null; }
        };

        assertDoesNotThrow(() -> event.applyEvent(Arrays.asList(winner, loser, nullTribePlayer), "sha_01"));

        // Era I: Vittoria = +5, Sconfitta = -3
        assertEquals(5, winner.getPP(), "Il vincitore deve avere 5 PP");
        assertEquals(-3, nullTribePlayer.getPP(), "Il perdente deve avere -3 PP");
    }

    @Test
    void testShamanicRitualEra2_BuildingsModifiers_AndTie() {
        ShamanicRitual event = new ShamanicRitual("sha_02", Era.II, 3, EventType.SHAMANIC_RITUAL, null);

        Player doubleWinner = new Player("Doppio Vincitore");
        doubleWinner.getTribe().addCharacter(new Shaman("s1", Era.I, 3, 2));

        // Edificio: Raddoppia i PP in caso di vittoria
        Building doublePPBuilding = new Building("b_01", Era.I, 3, 2, 0, new BaseDummyEffect() {
            @Override public boolean doublesShamanWinPP() { return true; }
        });
        doubleWinner.getTribe().addBuilding(doublePPBuilding);

        Player normalWinner = new Player("Vincitore Normale");
        normalWinner.getTribe().addCharacter(new Shaman("s2", Era.I, 3,  2)); // Pareggio!

        Player safeLoser = new Player("Perdente Protetto");
        safeLoser.getTribe().addCharacter(new Shaman("s3", Era.I, 3, 0));

        // Edificio: Previene la perdita di PP
        Building safeBuilding = new Building("b_02", Era.I, 3, 2, 0, new BaseDummyEffect() {
            @Override public boolean preventsShamanPPLoss() { return true; }
        });
        safeLoser.getTribe().addBuilding(safeBuilding);

        assertDoesNotThrow(() -> event.applyEvent(Arrays.asList(doubleWinner, normalWinner, safeLoser), "sha_02"));

        // Era II: Vittoria = +10, Sconfitta = -5
        assertEquals(20, doubleWinner.getPP(), "In pareggio vince, ma con l'edificio prende 10 * 2 = 20 PP");
        assertEquals(10, normalWinner.getPP(), "In pareggio vince e prende i normali 10 PP");
        assertEquals(0, safeLoser.getPP(), "Ha perso, ma l'edificio ha bloccato il -5 PP");
    }

    @Test
    void testShamanicRitualEra3_ExtraIconsBuilding_AndTotalTie() {
        ShamanicRitual event = new ShamanicRitual("sha_03", Era.III, 3, EventType.SHAMANIC_RITUAL, null);

        Player p1 = new Player("P1"); // 0 icone base
        // Edificio: 3 icone extra
        Building extraIconsBuilding = new Building("b_03", Era.I, 3, 2, 0, new BaseDummyEffect() {
            @Override public int getExtraShamanIcons() { return 3; }
        });
        p1.getTribe().addBuilding(extraIconsBuilding);

        Player p2 = new Player("P2"); // 3 icone base
        p2.getTribe().addCharacter(new Shaman("s1", Era.I, 3, 3));
        // Entrambi hanno 3 icone totali. Sono SIA i massimi che i minimi (Pareggio Totale).
        assertDoesNotThrow(() -> event.applyEvent(Arrays.asList(p1, p2), "sha_03"));

        // Era III: Vittoria = +15, Sconfitta = -7. Pareggio totale = prendono entrambi (15 - 7 = 8 PP)
        assertEquals(8, p1.getPP(), "Guadagna 15 e perde 7 = 8 PP");
        assertEquals(8, p2.getPP(), "Guadagna 15 e perde 7 = 8 PP");
    }

    @Test
    void testShamanicRitual_EmptyList() {
        // Verifica la riga 76: if (totalIconsPerPlayer.isEmpty()) return;
        ShamanicRitual event = new ShamanicRitual("sha_04", Era.I, 3, EventType.SHAMANIC_RITUAL, null);
        assertDoesNotThrow(() -> event.applyEvent(new ArrayList<>(), "sha_04"));
    }
}
