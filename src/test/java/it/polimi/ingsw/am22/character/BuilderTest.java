package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BuilderTest {

    @Test
    void testBuilderCreationGettersAndEffect() {
        // 1. Istanziamo l'oggetto Builder.
        // Oltre ai parametri standard, passiamo uno sconto cibo (es. 2) e i PP (es. 3)
        Builder builder = new Builder("build_01", Era.I, 3, "Builder", 2, 3);

        // Verifichiamo che l'istanza sia stata creata (copre il costruttore)
        assertNotNull(builder, "L'istanza di Builder non dovrebbe essere null");

        // 2. Verifichiamo che i Getter restituiscano i valori corretti (copre i metodi get)
        assertEquals(2, builder.getDiscountFood(), "Lo sconto cibo dovrebbe essere 2");
        assertEquals(3, builder.getPP(), "I Punti Prestigio dovrebbero essere 3");

        // 3. Prepariamo gli oggetti Player e Tribe necessari per il metodo
        Player dummyPlayer = new Player("Christian");
        Tribe dummyTribe = new Tribe();

        // 4. Eseguiamo applyImmediateEffect per assicurarci che non faccia nulla di inaspettato
        assertDoesNotThrow(() -> builder.applyImmediateEffect(dummyPlayer, dummyTribe),
                "applyImmediateEffect non deve lanciare eccezioni per la classe Builder");
    }
}