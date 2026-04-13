package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HunterTest {

    @Test
    void testHunterCreationAndNoIconEffect() {
        // 1. Creiamo un Hunter SENZA icona cibo (parametro booleano = false)
        Hunter hunterNoIcon = new Hunter("hunt_01", Era.I, 3, false);
        assertNotNull(hunterNoIcon, "L'istanza non dovrebbe essere null");
        assertFalse(hunterNoIcon.hasFoodIcon(), "hunterNoIcon non dovrebbe contenere FoodIcon");

        Player dummyPlayer = new Player("Christian");


        // 2. Eseguiamo l'effetto. Essendo false, l'if viene saltato.
        assertDoesNotThrow(() -> hunterNoIcon.applyImmediateEffect(dummyPlayer, dummyPlayer.getTribe()),
                "applyImmediateEffect non deve lanciare eccezioni");

        // (Opzionale) Se hai un metodo getFood(), puoi verificare che il cibo sia rimasto invariato
        assertEquals(0, dummyPlayer.getFood(), "Il cibo non deve aumentare se l'Hunter non ha l'icona");
    }

    @Test
    void testHunterWithIconEffect() {
        // 1. Creiamo un Hunter CON icona cibo (parametro booleano = true)
        Hunter hunterWithIcon = new Hunter("hunt_01", Era.I, 3, true);
        assertNotNull(hunterWithIcon);

        Player dummyPlayer = new Player("Christian");

        dummyPlayer.getTribe().addCharacter(hunterWithIcon);

        // 2. Eseguiamo l'effetto. Questa volta entrerà nell'if!
        assertDoesNotThrow(() -> hunterWithIcon.applyImmediateEffect(dummyPlayer, dummyPlayer.getTribe()),
                "L'aggiunta del cibo non deve lanciare eccezioni");

        // (Opzionale) Verifica effettiva che il cibo sia stato aggiunto!
        // Se l'Hunter è stato aggiunto alla tribù, countCharacters dovrebbe restituire almeno 1.
        assertTrue(dummyPlayer.getFood() > 0, "Il giocatore dovrebbe aver ricevuto cibo");
    }
}