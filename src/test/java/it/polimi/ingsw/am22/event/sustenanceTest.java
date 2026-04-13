package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class sustenanceTest {

    // Classe dummy per testare comodamente gli effetti degli edifici
    abstract class DummyBuildingEffect implements BuildingEffect {
        @Override public void applyEventBonus(EventType type, Player p, int count) {}
        public int getExtraShamanIcons() { return 0; }
        public boolean preventsShamanPPLoss() { return false; }
        public boolean doublesShamanWinPP() { return false; }
        public void onCharacterAdded(Player p, TribeCharacter c) {}

        // Questo è il metodo che ci interessa per questo test
        public int getSustenanceDiscount(Tribe tribe) { return 0; }
    }

    @Test
    void testSustenance_NullTribe() {
        // Copertura: if (tribe == null) continue;
        sustenance event = new sustenance("sust_00", Era.I, 3, EventType.SUSTENANCE, null);

        Player ghostPlayer = new Player("Fantasma") {
            @Override public Tribe getTribe() { return null; }
        };

        assertDoesNotThrow(() -> event.applyEvent(List.of(ghostPlayer), "sust_00"));
    }

    @Test
    void testSustenanceEra1_EnoughFood_NoDiscounts() {
        // Copertura: Era.I (PPLose = -1), Nessuno sconto, Cibo sufficiente
        sustenance event = new sustenance("sust_01", Era.I, 3, EventType.SUSTENANCE, null);

        Player p1 = new Player("Giocatore Cibo");
        // Supponiamo che il giocatore parta o venga settato con 5 cibo
        p1.addFood(5);

        // Aggiungiamo 3 personaggi normali (es. BUILDER, o qualsiasi cosa non sia COLLECTOR)
        p1.getTribe().addCharacter(new TribeCharacter("c1", Era.I, 3, CharacterType.BUILDER, null));
        p1.getTribe().addCharacter(new TribeCharacter("c2", Era.I, 3, CharacterType.BUILDER, null));
        p1.getTribe().addCharacter(new TribeCharacter("c3", Era.I, 3, CharacterType.BUILDER, null));

        event.applyEvent(List.of(p1), "sust_01");

        // Aveva 5 cibo, doveva sfamare 3 personaggi senza sconti.
        // Risultato atteso: 5 - 3 = 2 cibo rimanente. Nessuna perdita di PP.
        assertEquals(2, p1.getFood(), "Deve aver pagato 3 cibo, restando con 2");
        assertEquals(0, p1.getPP(), "Non deve perdere Punti Prestigio");
    }

    @Test
    void testSustenanceEra2_NotEnoughFood_WithCollectorDiscount() {
        // Copertura: Era.II (PPLose = -2), Sconto Raccoglitore, Cibo INSUFFICIENTE
        sustenance event = new sustenance("sust_02", Era.II, 3, EventType.SUSTENANCE, null);

        Player p2 = new Player("Giocatore Povero");
        p2.addFood(1); // Ha solo 1 cibo

        // Aggiungiamo 6 personaggi totali, di cui 1 COLLECTOR
        p2.getTribe().addCharacter(new TribeCharacter("c1", Era.I, 3, CharacterType.COLLECTOR, null)); // Sconto di 3
        for(int i=0; i<5; i++) {
            p2.getTribe().addCharacter(new TribeCharacter("c" + (i+2), Era.I, 3, CharacterType.BUILDER, null));
        }

        // Totale personaggi: 6. Sconto: 3 (1 Collector). Cibo da pagare = 6 - 3 = 3.
        // Ha solo 1 cibo. Cibo mancante = 3 - 1 = 2.
        // Penalità Era II = 2 (cibo mancante) * -2 = -4 PP.
        event.applyEvent(List.of(p2), "sust_02");

        assertEquals(0, p2.getFood(), "Deve aver azzerato il suo cibo");
        assertEquals(-4, p2.getPP(), "Deve aver perso 4 PP per non aver sfamato 2 personaggi nell'Era II");
    }

    @Test
    void testSustenanceEra3_NotEnoughFood_WithBuildingDiscount() {
        // Copertura: Era.III (PPLose = -3), Sconto Edificio, Cibo INSUFFICIENTE a 0
        sustenance event = new sustenance("sust_03", Era.III, 3, EventType.SUSTENANCE, null);

        Player p3 = new Player("Senza Cibo");
        // Parte con 0 cibo di default (nessuna addFood chiamata)

        // Aggiungiamo 2 personaggi normali
        p3.getTribe().addCharacter(new TribeCharacter("c1", Era.I, 3, CharacterType.BUILDER, null));
        p3.getTribe().addCharacter(new TribeCharacter("c2", Era.I, 3, CharacterType.BUILDER, null));

        // Aggiungiamo un edificio che dà 1 di sconto sul cibo
        Building foodBuilding = new Building("b1", Era.I, 3, 0, 0, new DummyBuildingEffect() {
            @Override public int getSustenanceDiscount(Tribe tribe) {
                return 1;
            }
        });
        p3.getTribe().addBuilding(foodBuilding);

        // Totale personaggi: 2. Sconto: 1 (Edificio). Cibo da pagare = 2 - 1 = 1.
        // Cibo posseduto = 0. Cibo mancante = 1.
        // Penalità Era III = 1 (cibo mancante) * -3 = -3 PP.
        event.applyEvent(List.of(p3), "sust_03");

        assertEquals(0, p3.getFood(), "Il cibo resta 0");
        assertEquals(-3, p3.getPP(), "Deve aver perso 3 PP per non aver sfamato 1 personaggio nell'Era III");
    }

    @Test
    void testSustenance_MaxZero_HugeDiscount() {
        // Copertura: Math.max(0, totalCharacters - totalDiscount)
        sustenance event = new sustenance("sust_04", Era.I, 3, EventType.SUSTENANCE, null);

        Player p4 = new Player("Giocatore Scontato");
        p4.addFood(2);

        // 1 Solo personaggio, ma è un COLLECTOR (Sconto = 3)
        p4.getTribe().addCharacter(new TribeCharacter("c1", Era.I, 3, CharacterType.COLLECTOR, null));

        // Cibo richiesto = max(0, 1 - 3) = 0. Non deve pagare nulla.
        event.applyEvent(List.of(p4), "sust_04");

        assertEquals(2, p4.getFood(), "Non deve aver speso cibo perché lo sconto copre tutto");
        assertEquals(0, p4.getPP(), "Nessuna penalità");
    }

    @Test
    void testSustenance_EmptyPlayersList() {
        // Copertura: Test robustezza con lista vuota (salta il for)
        sustenance event = new sustenance("sust_05", Era.I, 3, EventType.SUSTENANCE, null);
        assertDoesNotThrow(() -> event.applyEvent(new ArrayList<>(), "sust_05"));
    }
}