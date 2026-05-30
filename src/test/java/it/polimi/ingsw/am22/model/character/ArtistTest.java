package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArtistTest {

    @Test
    void testArtistCreation() {
        Artist artist = new Artist("art_01", Era.I, 3);
        assertNotNull(artist, "L'istanza di Artist non dovrebbe essere null");
        assertEquals(CharacterType.ARTIST, artist.getCharacterType());
    }
}
