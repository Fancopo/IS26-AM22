package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
class ArtistTest {

    @Test
    void testArtistCreationAndEffect() {
        // 1. Test del Costruttore (Copre le righe della dichiarazione della classe e del costruttore)
        Artist artist = new Artist('1', "Artist", 1, 2, "Artist");

        // Verifichiamo che l'oggetto sia stato istanziato correttamente
        assertNotNull(artist, "L'istanza di Artist non dovrebbe essere nulla");

        // 2. Test di applyImmediateEffect (Copre le righe del metodo vuoto)
        // Creiamo istanze dummy per i parametri. (Assicurati che Player e Tribe abbiano costruttori validi senza parametri o adattali di conseguenza)
        Player dummyPlayer = new Player("FANG");
        Tribe dummyTribe = new Tribe();

        // Eseguiamo il metodo. Utilizziamo assertDoesNotThrow per verificare che l'esecuzione del metodo vuoto non causi crash inaspettati.
        assertDoesNotThrow(() -> artist.applyImmediateEffect(dummyPlayer, dummyTribe),
                "applyImmediateEffect non dovrebbe lanciare eccezioni per la classe Artist");
    }
}