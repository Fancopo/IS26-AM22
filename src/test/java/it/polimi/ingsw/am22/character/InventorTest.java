package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventorTest {

    @Test
    void testInventorCreationGettersAndEffect() {
        // 1. Istanziamo l'oggetto Inventor.
        Inventor inventor = new Inventor("inv_01", Era.I, 3, "Inventor", 'A');

        // Verifichiamo che l'istanza sia stata creata (copre il costruttore)
        assertNotNull(inventor, "L'istanza di Inventor non dovrebbe essere null");

        // 2. Verifichiamo il Getter per l'icona (copre getIconPerInventor())
        assertEquals('A', inventor.getIconPerInventor(), "L'icona dovrebbe essere 'A'");

        // 3. Prepariamo gli oggetti Player e Tribe
        Player dummyPlayer = new Player("Christian");

        // 4. Eseguiamo applyImmediateEffect per assicurarci che non faccia nulla
        // e non lanci eccezioni (copre applyImmediateEffect())
        assertDoesNotThrow(() -> inventor.applyImmediateEffect(dummyPlayer, dummyPlayer.getTribe()),
                "applyImmediateEffect non deve lanciare eccezioni per la classe Inventor");
    }
}