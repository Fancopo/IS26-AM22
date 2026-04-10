package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtistTest {

    @Test
    void testArtistCreationAndEffect() {
        // 1. Istanziamo l'oggetto Artist reale usando il tuo costruttore.
        // I parametri sono: id, era, minPlayers, characterType
        Artist artist = new Artist("art_01", Era.I, 3, "Artist");

        // Verifichiamo che l'istanza sia stata creata correttamente (copre il costruttore)
        assertNotNull(artist, "L'istanza di Artist non dovrebbe essere null");

        // 2. Prepariamo gli oggetti Player e Tribe necessari per il metodo
        // (Assicurati che i costruttori di Player e Tribe corrispondano a quelli del tuo progetto)
        Player dummyPlayer = new Player("Christian");
        Tribe dummyTribe = new Tribe();

        // 3. Eseguiamo il metodo applyImmediateEffect.
        // Siccome il metodo è vuoto, testiamo semplicemente che la sua esecuzione
        // vada a buon fine senza lanciare eccezioni (copre il metodo applyImmediateEffect)
        assertDoesNotThrow(() -> artist.applyImmediateEffect(dummyPlayer, dummyTribe),
                "applyImmediateEffect non deve lanciare eccezioni per la classe Artist");
    }
}