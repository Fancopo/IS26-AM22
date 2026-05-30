package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollectorTest {

    @Test
    void testCollectorCreation() {
        Collector collector = new Collector("col_01", Era.I, 3);
        assertNotNull(collector, "L'istanza di Collector non dovrebbe essere null");
        assertEquals(CharacterType.COLLECTOR, collector.getCharacterType());
    }
}
