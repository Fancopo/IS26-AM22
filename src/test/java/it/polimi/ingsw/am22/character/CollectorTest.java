package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollectorTest {

    @Test
    void testCollectorCreationAndEffect() {
        // 1. Istanziamo l'oggetto Collector reale.
        // I parametri sono: id, era, minPlayers, characterType
        Collector collector = new Collector("col_01", Era.I, 3);

        // Verifichiamo che l'istanza sia stata creata correttamente (copre il costruttore)
        assertNotNull(collector, "L'istanza di Collector non dovrebbe essere null");

        // 2. Prepariamo gli oggetti Player e Tribe necessari per il metodo
        Player dummyPlayer = new Player("Christian");
        Tribe dummyTribe = new Tribe();

        // 3. Eseguiamo il metodo applyImmediateEffect.
        // Siccome i Raccoglitori agiscono solo nell'Evento Sostentamento, qui testiamo
        // solo che non esploda nulla eseguendo il metodo vuoto.
        assertDoesNotThrow(() -> collector.applyImmediateEffect(dummyPlayer, dummyTribe),
                "applyImmediateEffect non deve lanciare eccezioni per la classe Collector");
    }
}