package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HunterTest {

    @Test
    void hunterWithoutFoodIconShouldNotGrantFood() {
        Hunter hunterNoIcon = new Hunter("hunt_01", Era.I, 3, false);
        assertFalse(hunterNoIcon.hasFoodIcon());

        Player dummyPlayer = new Player("Christian");
        hunterNoIcon.addToTribe(dummyPlayer, dummyPlayer.getTribe());

        assertEquals(0, dummyPlayer.getFood(), "Hunter senza icona cibo non deve aggiungere cibo");
    }

    @Test
    void hunterWithFoodIconShouldGrantFoodEqualToHuntersInTribe() {
        Hunter first = new Hunter("hunt_01", Era.I, 3, false);
        Hunter triggering = new Hunter("hunt_02", Era.I, 3, true);

        Player dummyPlayer = new Player("Christian");
        first.addToTribe(dummyPlayer, dummyPlayer.getTribe());
        triggering.addToTribe(dummyPlayer, dummyPlayer.getTribe());

        assertEquals(2, dummyPlayer.getFood(),
                "Hunter con icona cibo deve aggiungere cibo pari al numero di cacciatori nella tribe");
    }
}
