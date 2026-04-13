package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShamanTest {

    @Test
    void testShamanCreationGettersAndEffect() {
        // 1. Istanziamo l'oggetto Shaman.
        // Oltre ai parametri standard, passiamo il numero di stelle/icone (es. 2)
        Shaman shaman = new Shaman("sha_01", Era.I, 3,  2);

        // Verifichiamo che l'istanza sia stata creata (copre il costruttore)
        assertNotNull(shaman, "L'istanza di Shaman non dovrebbe essere null");

        // 2. Verifichiamo il Getter per le stelle (copre getNumStars())
        assertEquals(2, shaman.getNumStars(), "Il numero di stelle dovrebbe essere 2");

        // 3. Prepariamo gli oggetti Player e Tribe
        Player dummyPlayer = new Player("Christian");

        // 4. Eseguiamo applyImmediateEffect per assicurarci che non lanci eccezioni
        assertDoesNotThrow(() -> shaman.applyImmediateEffect(dummyPlayer, dummyPlayer.getTribe()),
                "applyImmediateEffect non deve lanciare eccezioni per la classe Shaman");
    }
}