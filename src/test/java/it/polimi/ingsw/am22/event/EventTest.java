package it.polimi.ingsw.am22.event;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Game;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    // 1. DUMMY CLASSES PER ISOLARE IL TEST

    // Dummy per l'effetto: ci serve solo per verificare che venga effettivamente chiamato
    class DummyEventEffect implements EventEffect {
        boolean wasCalled = false;
        String passedId = "";

        @Override
        public void applyEvent(List<Player> players, String id) {
            this.wasCalled = true;
            this.passedId = id;
        }
    }



    // 2. I TEST

    @Test
    void testEventGettersAndBaseMethods() {
        // Copertura: Costruttore, getEventType, e applyEvent (che è vuoto)
        Event event = new Event("ev_01", Era.I, 3, EventType.SHAMANIC_RITUAL, null);

        assertEquals(EventType.SHAMANIC_RITUAL, event.getEventType(), "Il tipo di evento deve coincidere con quello passato al costruttore");

        // Verifichiamo che il metodo applyEvent padre non faccia crashare nulla
        assertDoesNotThrow(() -> event.applyEvent(new ArrayList<>(), "ev_01"), "Il metodo applyEvent di base dovrebbe essere vuoto e non lanciare eccezioni");
    }

    @Test
    void testGetTriggerPriority_Sustenance() {
        // Copertura: Ramo "true" dell'operatore ternario in getTriggerPriority
        Event event = new Event("ev_sust", Era.I, 3, EventType.SUSTENANCE, null);

        assertEquals(1, event.getTriggerPriority(), "L'evento SUSTENANCE deve avere priorità 1");
    }

    @Test
    void testGetTriggerPriority_OtherEvents() {
        // Copertura: Ramo "false" dell'operatore ternario in getTriggerPriority
        Event event = new Event("ev_shaman", Era.II, 3, EventType.SHAMANIC_RITUAL, null);

        assertEquals(0, event.getTriggerPriority(), "Tutti gli eventi non-SUSTENANCE devono avere priorità 0");
    }

    @Test
    void testAddToTribeThrowsException() {
        // Copertura: Il metodo addToTribe deve lanciare UnsupportedOperationException
        Event event = new Event("ev_02", Era.III, 3, EventType.SUSTENANCE, null);
        Player dummyPlayer = new Player("P1");
        Tribe dummyTribe = new Tribe();

        UnsupportedOperationException exception = assertThrows(
                UnsupportedOperationException.class,
                () -> event.addToTribe(dummyPlayer, dummyTribe)
        );

        assertEquals("An event cannot be added to the tribe", exception.getMessage());
    }

    @Test
    void testOnRoundEndTrigger() {
        // Copertura: Verifica che onRoundEndTrigger deleghi correttamente l'esecuzione a EventEffect
        DummyEventEffect dummyEffect = new DummyEventEffect();
        Event event = new Event("ev_trigger", Era.I, 3, EventType.SHAMANIC_RITUAL, dummyEffect);

        // Creiamo una lista di giocatori fittizia e un game dummy
        List<Player> fakePlayers = new ArrayList<>();
        fakePlayers.add(new Player("Tester"));
        Game dummyGame = new Game(fakePlayers);

        // Eseguiamo il trigger
        event.onRoundEndTrigger(dummyGame);

        // Verifiche
        assertTrue(dummyEffect.wasCalled, "L'effetto dell'evento doveva essere chiamato");
        assertEquals("ev_trigger", dummyEffect.passedId, "L'ID passato all'effetto deve corrispondere a quello dell'evento");
    }
}