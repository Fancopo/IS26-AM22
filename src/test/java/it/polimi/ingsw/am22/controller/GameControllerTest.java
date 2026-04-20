package it.polimi.ingsw.am22.controller;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameControllerTest {

    @Test
    void createGameWithDefaultTotemsShouldBuildInitialSnapshot() {
        GameController controller = new GameController();

        GameController.GameSnapshot snapshot = controller.createGameWithDefaultTotems(List.of("Alice", "Bob", "Carla"));

        assertTrue(controller.hasGame());
        assertEquals(1, snapshot.currentRound());
        assertEquals("Setup Iniziale", snapshot.currentPhase());
        assertEquals(3, snapshot.players().size());
        assertEquals(List.of("red", "blue", "green"),
                snapshot.players().stream().map(GameController.PlayerSnapshot::totemColor).toList());
        assertNull(snapshot.activePlayer());
    }

    @Test
    void startMatchShouldPopulateBoardAndSelectActivePlayer() {
        GameController controller = new GameController();
        controller.createGameWithDefaultTotems(List.of("Alice", "Bob"));

        GameController.GameSnapshot snapshot = controller.startMatch();

        assertEquals("Piazzamento Totem", snapshot.currentPhase());
        assertNotNull(snapshot.activePlayer());
        assertEquals(7, snapshot.board().upperRow().size());
        assertEquals(3, snapshot.board().lowerRow().size());
        assertEquals(2, snapshot.board().turnOrder().stream()
                .filter(slot -> slot.occupiedBy() != null)
                .count());
    }

    @Test
    void placeTotemShouldRejectActionsOutsideTheActiveTurn() {
        GameController controller = new GameController();
        controller.createGameWithDefaultTotems(List.of("Alice", "Bob"));
        GameController.GameSnapshot afterStart = controller.startMatch();

        String wrongPlayer = afterStart.players().stream()
                .map(GameController.PlayerSnapshot::nickname)
                .filter(name -> !name.equals(afterStart.activePlayer()))
                .findFirst()
                .orElseThrow();

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> controller.placeTotem(wrongPlayer, 'B')
        );

        assertEquals("It is not " + wrongPlayer + "'s turn.", exception.getMessage());
    }

    @Test
    void controllerShouldDriveACompleteTwoPlayerRound() {
        GameController controller = new GameController();
        controller.createGameWithDefaultTotems(List.of("Alice", "Bob"));

        GameController.GameSnapshot afterStart = controller.startMatch();
        String firstPlayer = afterStart.activePlayer();

        GameController.GameSnapshot afterFirstPlacement = controller.placeTotem(firstPlayer, 'B');
        String secondPlayer = afterFirstPlacement.activePlayer();

        assertNotEquals(firstPlayer, secondPlayer);
        assertEquals(firstPlayer, offerTile(afterFirstPlacement, 'B').occupiedBy());
        assertEquals(1, afterFirstPlacement.board().turnOrder().stream()
                .filter(slot -> slot.occupiedBy() != null)
                .count());

        GameController.GameSnapshot afterSecondPlacement = controller.placeTotem(secondPlayer, 'C');
        assertEquals("Risoluzione Azioni", afterSecondPlacement.currentPhase());
        assertEquals(firstPlayer, afterSecondPlacement.activePlayer());

        String firstPickId = firstSelectableCard(afterSecondPlacement.board().lowerRow()).id();
        GameController.GameSnapshot afterFirstPick = controller.pickCards(firstPlayer, List.of(firstPickId));

        assertEquals("Risoluzione Azioni", afterFirstPick.currentPhase());
        assertEquals(secondPlayer, afterFirstPick.activePlayer());

        String secondPickId = firstSelectableCard(afterFirstPick.board().upperRow()).id();
        GameController.GameSnapshot afterSecondPick = controller.pickCards(secondPlayer, List.of(secondPickId));

        assertEquals(2, afterSecondPick.currentRound());
        assertEquals("Piazzamento Totem", afterSecondPick.currentPhase());
        assertEquals(1, ownedCardCount(afterSecondPick, firstPlayer));
        assertEquals(1, ownedCardCount(afterSecondPick, secondPlayer));
    }

    private GameController.OfferTileSnapshot offerTile(GameController.GameSnapshot snapshot, char letter) {
        return snapshot.board().offerTrack().stream()
                .filter(tile -> tile.letter() == letter)
                .findFirst()
                .orElseThrow();
    }

    private GameController.CardSnapshot firstSelectableCard(List<GameController.CardSnapshot> cards) {
        return cards.stream()
                .filter(card -> !"EVENT".equals(card.category()))
                .filter(card -> card.foodCost() == 0)
                .findFirst()
                .orElseThrow();
    }

    private int ownedCardCount(GameController.GameSnapshot snapshot, String nickname) {
        GameController.PlayerSnapshot player = snapshot.players().stream()
                .filter(candidate -> candidate.nickname().equals(nickname))
                .findFirst()
                .orElseThrow();

        return player.tribeCharacters().size() + player.buildings().size();
    }
}
