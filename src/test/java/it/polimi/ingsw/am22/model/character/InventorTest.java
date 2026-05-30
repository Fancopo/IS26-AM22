package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventorTest {

    @Test
    void testInventorCreationAndGetters() {
        Inventor inventor = new Inventor("inv_01", Era.I, 3, 'A');

        assertNotNull(inventor, "L'istanza di Inventor non dovrebbe essere null");
        assertEquals('A', inventor.getIconPerInventor(), "L'icona dovrebbe essere 'A'");
    }
}
