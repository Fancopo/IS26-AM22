package it.polimi.ingsw.am22.model;

import it.polimi.ingsw.am22.model.Building.Building;
import it.polimi.ingsw.am22.model.Building.BuildingEffect;
import it.polimi.ingsw.am22.model.character.CharacterType;
import it.polimi.ingsw.am22.model.character.TribeCharacter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerTest {

    @Test
    void shouldInitializePlayerCorrectly() {
        Player player = new Player("Alice");

        assertEquals("Alice", player.getNickname());
        assertEquals(0, player.getPP());
        assertEquals(0, player.getFood());
        assertNotNull(player.getTribe());
        assertNull(player.getTotem());
    }

    @Test
    void setTotemShouldAssignTotem() {
        Player player = new Player("Alice");
        Totem totem = mock(Totem.class);

        player.setTotem(totem);

        assertEquals(totem, player.getTotem());
    }

    @Test
    void addFoodShouldIncreaseFood() {
        Player player = new Player("Alice");

        player.addFood(3);

        assertEquals(3, player.getFood());
    }

    @Test
    void addFoodShouldAllowNegativeAmountsIfUsedAsGenericAdder() {
        Player player = new Player("Alice");
        player.addFood(5);

        player.addFood(-2);

        assertEquals(3, player.getFood());
    }

    @Test
    void payFoodShouldDecreaseFood() {
        Player player = new Player("Alice");
        player.addFood(5);

        player.payFood(2);

        assertEquals(3, player.getFood());
    }

    @Test
    void payFoodShouldAllowPayingExactAmount() {
        Player player = new Player("Alice");
        player.addFood(4);

        player.payFood(4);

        assertEquals(0, player.getFood());
    }

    @Test
    void payFoodShouldDoNothingWhenAmountIsZero() {
        Player player = new Player("Alice");
        player.addFood(4);

        player.payFood(0);

        assertEquals(4, player.getFood());
    }

    @Test
    void payFoodShouldThrowIfAmountIsNegative() {
        Player player = new Player("Alice");

        assertThrows(IllegalArgumentException.class, () -> player.payFood(-1));
    }

    @Test
    void payFoodShouldThrowIfFoodIsInsufficient() {
        Player player = new Player("Alice");
        player.addFood(1);

        assertThrows(IllegalStateException.class, () -> player.payFood(2));
        assertEquals(1, player.getFood());
    }

    @Test
    void addPPShouldIncreasePrestigePoints() {
        Player player = new Player("Alice");

        player.addPP(4);

        assertEquals(4, player.getPP());
    }

    @Test
    void addPPShouldAllowNegativePrestigePoints() {
        Player player = new Player("Alice");
        player.addPP(5);

        player.addPP(-2);

        assertEquals(3, player.getPP());
    }

    @Test
    void hasExtraBuyAtRoundShouldReturnFalseWhenNoBuildingGrantsIt() {
        Player player = new Player("Alice");

        assertFalse(player.hasExtraBuyAtRoundEnd());
    }

    @Test
    void hasExtraBuyAtRoundShouldReturnTrueWhenOneBuildingGrantsIt() {
        Player player = new Player("Alice");
        Building building = mock(Building.class);

        when(building.grantsExtraBuyAtRoundEnd()).thenReturn(true);
        player.getTribe().addBuilding(building);

        assertTrue(player.hasExtraBuyAtRoundEnd());
    }

    @Test
    void finalPPShouldReturnBasePPWhenNoExtraEndgamePointsExist() {
        Player player = new Player("Alice");
        player.addPP(7);

        assertEquals(7, player.finalPP());
    }

    @Test
    void finalPPShouldIncludeBuilderPrintedPP() {
        Player player = new Player("Alice");

        TribeCharacter builder = mock(TribeCharacter.class);
        when(builder.getCharacterType()).thenReturn(CharacterType.BUILDER);
        when(builder.getPP()).thenReturn(3);

        player.getTribe().addCharacter(builder);

        assertEquals(3, player.finalPP());
    }

    @Test
    void finalPPShouldIncludeInventorBonus() {
        Player player = new Player("Alice");

        TribeCharacter inventor1 = mock(TribeCharacter.class);
        when(inventor1.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor1.getIconPerInventor()).thenReturn('A');

        TribeCharacter inventor2 = mock(TribeCharacter.class);
        when(inventor2.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor2.getIconPerInventor()).thenReturn('B');

        player.getTribe().addCharacter(inventor1);
        player.getTribe().addCharacter(inventor2);

        assertEquals(4, player.finalPP()); // 2 inventori * 2 icone diverse
    }

    @Test
    void finalPPShouldIncludeArtistBonus() {
        Player player = new Player("Alice");

        TribeCharacter artist1 = mock(TribeCharacter.class);
        when(artist1.getCharacterType()).thenReturn(CharacterType.ARTIST);

        TribeCharacter artist2 = mock(TribeCharacter.class);
        when(artist2.getCharacterType()).thenReturn(CharacterType.ARTIST);

        player.getTribe().addCharacter(artist1);
        player.getTribe().addCharacter(artist2);

        assertEquals(10, player.finalPP());
    }

    @Test
    void finalPPShouldIncludeBuildingEffectEndgamePoints() {
        Player player = new Player("Alice");
        Building building = mock(Building.class);
        BuildingEffect effect = mock(BuildingEffect.class);

        when(building.getFinalPP()).thenReturn(2);
        when(building.getEffect()).thenReturn(effect);
        when(effect.calculateEndGame(player.getTribe())).thenReturn(5);

        player.getTribe().addBuilding(building);

        assertEquals(7, player.finalPP());
    }
    @Test
    void finalPPShouldCombineAllSourcesOfPoints() {
        Player player = new Player("Alice");
        player.addPP(5);

        TribeCharacter builder = mock(TribeCharacter.class);
        when(builder.getCharacterType()).thenReturn(CharacterType.BUILDER);
        when(builder.getPP()).thenReturn(3);

        TribeCharacter inventor1 = mock(TribeCharacter.class);
        when(inventor1.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor1.getIconPerInventor()).thenReturn('A');

        TribeCharacter inventor2 = mock(TribeCharacter.class);
        when(inventor2.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor2.getIconPerInventor()).thenReturn('B');

        TribeCharacter artist1 = mock(TribeCharacter.class);
        when(artist1.getCharacterType()).thenReturn(CharacterType.ARTIST);

        TribeCharacter artist2 = mock(TribeCharacter.class);
        when(artist2.getCharacterType()).thenReturn(CharacterType.ARTIST);

        Building building = mock(Building.class);
        BuildingEffect effect = mock(BuildingEffect.class);

        when(building.getFinalPP()).thenReturn(7);
        when(building.getEffect()).thenReturn(effect);
        when(effect.calculateEndGame(player.getTribe())).thenReturn(0);

        player.getTribe().addCharacter(builder);
        player.getTribe().addCharacter(inventor1);
        player.getTribe().addCharacter(inventor2);
        player.getTribe().addCharacter(artist1);
        player.getTribe().addCharacter(artist2);
        player.getTribe().addBuilding(building);

        assertEquals(29, player.finalPP());
    }

    @Test
    void finalPPShouldCountDuplicateInventorIconsOnlyOnce() {
        Player player = new Player("Alice");

        TribeCharacter inventor1 = mock(TribeCharacter.class);
        when(inventor1.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor1.getIconPerInventor()).thenReturn('A');

        TribeCharacter inventor2 = mock(TribeCharacter.class);
        when(inventor2.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor2.getIconPerInventor()).thenReturn('A');

        player.getTribe().addCharacter(inventor1);
        player.getTribe().addCharacter(inventor2);

        assertEquals(2, player.finalPP());
    }

    @Test
    void finalPPShouldNotGiveArtistBonusWithOnlyOneArtist() {
        Player player = new Player("Alice");

        TribeCharacter artist = mock(TribeCharacter.class);
        when(artist.getCharacterType()).thenReturn(CharacterType.ARTIST);

        player.getTribe().addCharacter(artist);

        assertEquals(0, player.finalPP());
    }

    @Test
    void finalPPShouldGiveOnlyTenPointsWithThreeArtists() {
        Player player = new Player("Alice");

        TribeCharacter artist1 = mock(TribeCharacter.class);
        when(artist1.getCharacterType()).thenReturn(CharacterType.ARTIST);

        TribeCharacter artist2 = mock(TribeCharacter.class);
        when(artist2.getCharacterType()).thenReturn(CharacterType.ARTIST);

        TribeCharacter artist3 = mock(TribeCharacter.class);
        when(artist3.getCharacterType()).thenReturn(CharacterType.ARTIST);

        player.getTribe().addCharacter(artist1);
        player.getTribe().addCharacter(artist2);
        player.getTribe().addCharacter(artist3);

        assertEquals(10, player.finalPP());
    }

}