package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShamanTest {

    @Test
    void testShamanCreationAndGetters() {
        Shaman shaman = new Shaman("sha_01", Era.I, 3, 2);

        assertNotNull(shaman, "L'istanza di Shaman non dovrebbe essere null");
        assertEquals(2, shaman.getNumStars(), "Il numero di stelle dovrebbe essere 2");
    }
}
