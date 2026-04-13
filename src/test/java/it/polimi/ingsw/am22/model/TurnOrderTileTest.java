package it.polimi.ingsw.am22.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnOrderTileTest {

    private TurnOrderTile tile;

    @BeforeEach
    void setUp() {
        // Inizializziamo una nuova tile prima di ogni test
        tile = new TurnOrderTile();
    }

    @Test
    void testConstructorAndGetSlots() {
        // Copertura: Costruttore e getSlots() iniziale
        assertNotNull(tile.getSlots(), "La lista degli slot non deve essere null");
        assertTrue(tile.getSlots().isEmpty(), "La lista degli slot deve essere vuota appena creata");
    }

    @Test
    void testSetup() {
        // Copertura: Metodo setup(int playerCount), creazione Slot, posizione e isLastSpace
        int playerCount = 3;
        tile.setup(playerCount);

        List<Slot> slots = tile.getSlots();
        assertEquals(3, slots.size(), "Deve aver creato esattamente 3 slot");

        // Controllo Slot 1
        // Nota: Assumo che tu abbia i getter getPosition() e isLastSpace() nella classe Slot.
        // Se non li hai, questa parte di test assicura che tu li crei per verificare lo stato interno!
        assertEquals(1, slots.get(0).getPositionIndex(), "Il primo slot deve avere posizione 1");
        assertFalse(slots.get(0).isLastSpace(), "Il primo slot NON deve essere l'ultimo");

        // Controllo Slot 2
        assertEquals(2, slots.get(1).getPositionIndex(), "Il secondo slot deve avere posizione 2");
        assertFalse(slots.get(1).isLastSpace(), "Il secondo slot NON deve essere l'ultimo");

        // Controllo Slot 3 (L'ultimo)
        assertEquals(3, slots.get(2).getPositionIndex(), "Il terzo slot deve avere posizione 3");
        assertTrue(slots.get(2).isLastSpace(), "Il terzo slot DEVE essere contrassegnato come ultimo (penalità)");
    }

    @Test
    void testGetFirstAvailableSlot_AllEmpty() {
        // Copertura: getFirstAvailableSlot() quando nessuno slot è occupato
        tile.setup(2);

        Slot available = tile.getFirstAvailableSlot();
        assertNotNull(available, "Deve trovare uno slot disponibile");
        assertEquals(1, available.getPositionIndex(), "Deve restituire il primissimo slot della lista");
    }

    @Test
    void testGetFirstAvailableSlot_PartiallyOccupied() {
        // Copertura: getFirstAvailableSlot() quando il primo slot è già preso
        tile.setup(3);
        List<Slot> slots = tile.getSlots();
        Player p1 = new Player("P1");

        // Simuliamo l'occupazione del primo slot
        Totem t1 = new Totem("white", p1); // Assumo esista un costruttore vuoto o simile
        slots.get(0).placeTotem(t1); // -> ADATTA IL NOME DI QUESTO METODO ALLA TUA CLASSE SLOT

        Slot available = tile.getFirstAvailableSlot();
        assertNotNull(available, "Deve trovare il secondo slot disponibile");
        assertEquals(2, available.getPositionIndex(), "Deve restituire il secondo slot, poiché il primo è occupato");
    }

    @Test
    void testGetFirstAvailableSlot_AllFull() {
        // Copertura: getFirstAvailableSlot() e il suo return null
        tile.setup(2);
        List<Slot> slots = tile.getSlots();
        Player p1 = new Player("P1");
        Player p2 = new Player("P2");

        // Occupiamo tutti gli slot
        Totem t1 = new Totem("white", p1);
        Totem t2 = new Totem("black", p2);
        slots.get(0).placeTotem(t1);
        slots.get(1).placeTotem(t2);

        Slot available = tile.getFirstAvailableSlot();
        assertNull(available, "Deve restituire null se tutti gli slot sono occupati");
    }

    @Test
    void testGetTurnOrderAndOccupiedSlotsCount() {
        // Copertura: getTurnOrder() e getOccupiedSlotsCount()
        tile.setup(4);
        List<Slot> slots = tile.getSlots();

        // Inizialmente 0 occupati
        assertEquals(0, tile.getOccupiedSlotsCount(), "Nessuno slot deve essere occupato all'inizio");
        assertTrue(tile.getTurnOrder().isEmpty(), "L'ordine di turno deve essere vuoto");
        Player p1 = new Player("P1");
        Player p2 = new Player("P2");

        // Aggiungiamo 2 totem (su slot 1 e 3)
        Totem t1 = new Totem("white", p1);
        Totem t2 = new Totem("black", p2);
        slots.get(0).placeTotem(t1);
        slots.get(2).placeTotem(t2);

        // Verifiche
        assertEquals(2, tile.getOccupiedSlotsCount(), "Devono risultare esattamente 2 slot occupati");

        List<Totem> order = tile.getTurnOrder();
        assertEquals(2, order.size(), "L'ordine di turno deve contenere 2 totem");

        // Verifica l'ordine effettivo (deve saltare lo slot vuoto)
        assertEquals(t1, order.get(0), "Il primo totem in ordine deve essere t1");
        assertEquals(t2, order.get(1), "Il secondo totem in ordine deve essere t2");
    }
}