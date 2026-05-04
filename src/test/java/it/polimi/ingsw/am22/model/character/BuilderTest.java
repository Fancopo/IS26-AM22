package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    @Test
    void testBuilderCreationAndGetters() {
        Builder builder = new Builder("build_01", Era.I, 3, 2, 3);

        assertNotNull(builder, "L'istanza di Builder non dovrebbe essere null");
        assertEquals(2, builder.getDiscountFood(), "Lo sconto cibo dovrebbe essere 2");
        assertEquals(3, builder.getPP(), "I Punti Prestigio dovrebbero essere 3");
    }
}
