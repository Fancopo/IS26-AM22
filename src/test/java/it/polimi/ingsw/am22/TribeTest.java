package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TribeTest {

    @Test
    void shouldInitializeEmptyTribe() {
        Tribe tribe = new Tribe();

        assertNotNull(tribe.getMembers());
        assertNotNull(tribe.getBuildings());
        assertTrue(tribe.getMembers().isEmpty());
        assertTrue(tribe.getBuildings().isEmpty());
    }

    @Test
    void addCardShouldRejectNull() {
        Tribe tribe = new Tribe();
        Player player = mock(Player.class);

        assertThrows(IllegalArgumentException.class, () -> tribe.addCard(player, null));
    }
    @Test
    void addCardShouldDelegateToCard() {
        Tribe tribe = new Tribe();
        Player player = mock(Player.class);
        Card card = mock(Card.class);

        tribe.addCard(player, card);

        verify(card).addToTribe(player, tribe);
    }

    @Test
    void addCharacterShouldAddCharacter() {
        Tribe tribe = new Tribe();
        TribeCharacter character = mock(TribeCharacter.class);

        tribe.addCharacter(character);

        assertEquals(1, tribe.getMembers().size());
        assertTrue(tribe.getMembers().contains(character));
    }

    @Test
    void addCharacterShouldRejectNull() {
        Tribe tribe = new Tribe();

        assertThrows(IllegalArgumentException.class, () -> tribe.addCharacter(null));
    }

    @Test
    void addBuildingShouldAddBuilding() {
        Tribe tribe = new Tribe();
        Building building = mock(Building.class);

        tribe.addBuilding(building);

        assertEquals(1, tribe.getBuildings().size());
        assertTrue(tribe.getBuildings().contains(building));
    }

    @Test
    void addBuildingShouldRejectNull() {
        Tribe tribe = new Tribe();

        assertThrows(IllegalArgumentException.class, () -> tribe.addBuilding(null));
    }
    @Test
    void countCharactersShouldReturnCorrectNumber() {
        Tribe tribe = new Tribe();

        TribeCharacter hunter1 = mock(TribeCharacter.class);
        when(hunter1.getCharacterType()).thenReturn(CharacterType.HUNTER);

        TribeCharacter hunter2 = mock(TribeCharacter.class);
        when(hunter2.getCharacterType()).thenReturn(CharacterType.HUNTER);

        TribeCharacter builder = mock(TribeCharacter.class);
        when(builder.getCharacterType()).thenReturn(CharacterType.BUILDER);

        tribe.addCharacter(hunter1);
        tribe.addCharacter(hunter2);
        tribe.addCharacter(builder);

        assertEquals(2, tribe.countCharacters(CharacterType.HUNTER));
        assertEquals(1, tribe.countCharacters(CharacterType.BUILDER));
        assertEquals(0, tribe.countCharacters(CharacterType.ARTIST));
    }

    @Test
    void countUniqueInventorIconsShouldReturnZeroForEmptyTribe() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.countUniqueInventorIcons());
    }

    @Test
    void countUniqueInventorIconsShouldIgnoreNonInventors() {
        Tribe tribe = new Tribe();

        TribeCharacter hunter = mock(TribeCharacter.class);
        when(hunter.getCharacterType()).thenReturn(CharacterType.HUNTER);

        tribe.addCharacter(hunter);

        assertEquals(0, tribe.countUniqueInventorIcons());
    }

    @Test
    void countUniqueInventorIconsShouldCountDistinctIconsOnly() {
        Tribe tribe = new Tribe();

        TribeCharacter inventor1 = mock(TribeCharacter.class);
        when(inventor1.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor1.getIconPerInventor()).thenReturn('A');

        TribeCharacter inventor2 = mock(TribeCharacter.class);
        when(inventor2.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor2.getIconPerInventor()).thenReturn('A');

        TribeCharacter inventor3 = mock(TribeCharacter.class);
        when(inventor3.getCharacterType()).thenReturn(CharacterType.INVENTOR);
        when(inventor3.getIconPerInventor()).thenReturn('B');

        tribe.addCharacter(inventor1);
        tribe.addCharacter(inventor2);
        tribe.addCharacter(inventor3);

        assertEquals(2, tribe.countUniqueInventorIcons());
    }

    @Test
    void getBuilderDiscountShouldReturnZeroWhenNoBuildersExist() {
        Tribe tribe = new Tribe();

        assertEquals(0, tribe.getBuilderDiscount());
    }

    @Test
    void getBuilderDiscountShouldSumDiscountsOfAllBuilders() {
        Tribe tribe = new Tribe();

        TribeCharacter builder1 = mock(TribeCharacter.class);
        when(builder1.getCharacterType()).thenReturn(CharacterType.BUILDER);
        when(builder1.getDiscountFood()).thenReturn(1);

        TribeCharacter builder2 = mock(TribeCharacter.class);
        when(builder2.getCharacterType()).thenReturn(CharacterType.BUILDER);
        when(builder2.getDiscountFood()).thenReturn(2);

        TribeCharacter hunter = mock(TribeCharacter.class);
        when(hunter.getCharacterType()).thenReturn(CharacterType.HUNTER);

        tribe.addCharacter(builder1);
        tribe.addCharacter(builder2);
        tribe.addCharacter(hunter);

        assertEquals(3, tribe.getBuilderDiscount());
    }

    @Test
    void hasExtraBuyAtRoundEndShouldReturnFalseWhenNoBuildingsExist() {
        Tribe tribe = new Tribe();

        assertFalse(tribe.hasExtraBuyAtRoundEnd());
    }

    @Test
    void hasExtraBuyAtRoundEndShouldReturnFalseWhenNoBuildingGrantsIt() {
        Tribe tribe = new Tribe();
        Building building = mock(Building.class);

        when(building.grantsExtraBuyAtRoundEnd()).thenReturn(false);
        tribe.addBuilding(building);

        assertFalse(tribe.hasExtraBuyAtRoundEnd());
    }

    @Test
    void hasExtraBuyAtRoundEndShouldReturnTrueWhenAtLeastOneBuildingGrantsIt() {
        Tribe tribe = new Tribe();

        Building building1 = mock(Building.class);
        when(building1.grantsExtraBuyAtRoundEnd()).thenReturn(false);

        Building building2 = mock(Building.class);
        when(building2.grantsExtraBuyAtRoundEnd()).thenReturn(true);

        tribe.addBuilding(building1);
        tribe.addBuilding(building2);

        assertTrue(tribe.hasExtraBuyAtRoundEnd());
    }

    @Test
    void getMembersShouldReturnUnmodifiableList() {
        Tribe tribe = new Tribe();
        TribeCharacter character = mock(TribeCharacter.class);
        tribe.addCharacter(character);

        assertThrows(UnsupportedOperationException.class,
                () -> tribe.getMembers().add(mock(TribeCharacter.class)));
    }

    @Test
    void getBuildingsShouldReturnUnmodifiableList() {
        Tribe tribe = new Tribe();
        Building building = mock(Building.class);
        tribe.addBuilding(building);

        assertThrows(UnsupportedOperationException.class,
                () -> tribe.getBuildings().add(mock(Building.class)));
    }
}
