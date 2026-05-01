package it.polimi.ingsw.am22.model.character;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.building.BuildingEffect;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TribeCharacterTest {

    @Test
    void testTribeCharacterCreationAndDummyGetters() {
        // 1. Testiamo la creazione base e il getter del tipo
        CharacterEffect effect = null;
        TribeCharacter character = new TribeCharacter("tc_01", Era.I, 3, CharacterType.ARTIST, effect);
        assertNotNull(character, "L'istanza non dovrebbe essere null");
        assertEquals(CharacterType.ARTIST, character.getCharacterType(), "Il tipo dovrebbe essere ARTIST");

        // 2. Testiamo i getter fittizi della superclasse per la coverage
        assertEquals(0, character.getNumStars());
        assertEquals('0', character.getIconPerInventor());
        assertEquals(0, character.getDiscountFood());
        assertEquals(0, character.getPP());
        assertNull(character.getEffect());
    }

    @Test
    void testAddToTribeWithoutBuildings() {
        // Test base: Aggiunta a una tribù vuota
        CharacterEffect effect = null;
        TribeCharacter character = new TribeCharacter("tc_02", Era.I, 3, CharacterType.HUNTER, null);
        Player dummyPlayer = new Player("Christian");

        assertDoesNotThrow(() -> character.addToTribe(dummyPlayer, dummyPlayer.getTribe()));

        // Verifichiamo che sia stato effettivamente aggiunto alla lista dei membri
        assertTrue(dummyPlayer.getTribe().getMembers().contains(character), "Il personaggio deve essere nei membri della tribù");
    }

    @Test
    void testAddToTribeWithBuildings() {
        CharacterEffect effect = null;
        TribeCharacter character = new TribeCharacter("tc_03", Era.I, 3, CharacterType.SHAMAN, effect);
        Player dummyPlayer = new Player("Christian");


        // 1. Creiamo un edificio SENZA effetto (passando null come effetto)
        // ATTENZIONE: Modifica i parametri di "new Building(...)" con quelli reali del tuo progetto!
        Building buildingNoEffect = new Building("b_01", Era.I, 3, 2, 5, null);
        dummyPlayer.getTribe().addBuilding(buildingNoEffect);

        // 2. Creiamo un edificio CON effetto.
        // Creiamo un effetto fittizio "al volo" implementando l'interfaccia/classe.
        BuildingEffect dummyEffect = new BuildingEffect() {
            @Override
            public void onCharacterAdded(Player player, TribeCharacter newChar) {
                // Corpo vuoto: ci serve solo che il metodo esista e venga chiamato senza esplodere
            }
        };
        // ATTENZIONE: Anche qui modifica i parametri del Building!
        Building buildingWithEffect = new Building("b_02", Era.I, 3, 2, 5, dummyEffect);
        dummyPlayer.getTribe().addBuilding(buildingWithEffect);

        // -- ESEGUIAMO IL METODO --
        // Ora il ciclo 'for' troverà edifici, e l'if controllerà sia il ramo true che false.
        assertDoesNotThrow(() -> character.addToTribe(dummyPlayer, dummyPlayer.getTribe()));

        // Verifichiamo che sia stato aggiunto
        assertTrue(dummyPlayer.getTribe().getMembers().contains(character));
    }
}
