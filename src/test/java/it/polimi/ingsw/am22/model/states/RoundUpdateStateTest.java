package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Slot;
import it.polimi.ingsw.am22.model.Totem;
import it.polimi.ingsw.am22.model.Tribe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundUpdateStateTest {

    private List<Player> players;
    private final RoundUpdateState state = new RoundUpdateState();

    @Test
    void normalRoundReordersPlayersAdvancesRoundAndReturnsToPlacement() {
        Game game = newGame(2);
        // Seat the totems in reversed turn order: P2 first, then P1.
        placeInSlot(game, players.get(1), 0);
        placeInSlot(game, players.get(0), 1);
        fillDeck(game, 6);

        game.setState(state);
        game.updateRound();

        assertEquals(List.of(players.get(1), players.get(0)), game.getPlayers(),
                "Players are reordered to match the turn-order tile");
        assertSame(players.get(1), game.getActivePlayer(), "First in the new order acts next");
        assertEquals(2, game.getCurrentRound(), "Round advances");
        assertInstanceOf(TotemPlacementState.class, game.getCurrentState());
    }

    @Test
    void finalRoundEndsTheGameWithoutAdvancingTheRound() {
        Game game = newGame(2);
        placeInSlot(game, players.get(0), 0);
        placeInSlot(game, players.get(1), 1);
        fillDeck(game, 6);
        game.setCurrentRound(10);

        game.setState(state);
        game.updateRound();

        assertInstanceOf(EndGameState.class, game.getCurrentState());
        assertTrue(game.isGameEnded());
        assertEquals(10, game.getCurrentRound(), "Round 10 is the last one");
    }

    @Test
    void cleansAndShiftsBoardRowsBeforeRefilling() {
        Game game = newGame(2);
        placeInSlot(game, players.get(0), 0);
        placeInSlot(game, players.get(1), 1);

        Card survivor = stub("survivor", Era.I, true);
        Card dead = stub("dead", Era.I, false);
        game.getBoard().getLowerRow().add(survivor);
        game.getBoard().getLowerRow().add(dead);

        Card building = stub("building", Era.I, true);
        Card event = stub("event", Era.I, false);
        game.getBoard().getUpperRow().add(building);
        game.getBoard().getUpperRow().add(event);

        fillDeck(game, 6);

        game.setState(state);
        game.updateRound();

        // lower: dead removed, event shifted down from the upper row
        assertEquals(2, game.getBoard().getLowerRow().size());
        assertTrue(game.getBoard().getLowerRow().contains(survivor));
        assertTrue(game.getBoard().getLowerRow().contains(event));
        assertFalse(game.getBoard().getLowerRow().contains(dead));

        // upper: building stayed, plus the 6 refilled cards
        assertEquals(7, game.getBoard().getUpperRow().size());
        assertTrue(game.getBoard().getUpperRow().contains(building));
    }

    @Test
    void refillingAHigherEraCardAdvancesTheEra() {
        Game game = newGame(2);
        placeInSlot(game, players.get(0), 0);
        placeInSlot(game, players.get(1), 1);

        List<Card> deck = new ArrayList<>();
        deck.add(stub("eraII", Era.II, false));
        for (int i = 0; i < 5; i++) deck.add(stub("n" + i, Era.I, false));
        game.getTribeDeck().addAll(deck);

        assertEquals(Era.I, game.getCurrentEra());

        game.setState(state);
        game.updateRound();

        assertEquals(Era.II, game.getCurrentEra());
    }

    @Test
    void exposesPhaseName() {
        assertEquals("End of Round / Cleanup", state.getPhaseName());
    }

    // ==================== helpers ====================

    private Game newGame(int playerCount) {
        players = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            Player p = new Player("P" + i);
            p.setTotem(new Totem("color" + i, p));
            players.add(p);
        }
        Game game = new Game(players);
        game.getBoard().getTurnOrderTile().setup(playerCount);
        return game;
    }

    private void placeInSlot(Game game, Player player, int slotIndex) {
        Slot slot = game.getBoard().getTurnOrderTile().getSlots().get(slotIndex);
        player.getTotem().moveToTurnOrder(slot);
    }

    private void fillDeck(Game game, int count) {
        for (int i = 0; i < count; i++) {
            game.getTribeDeck().add(stub("deck" + i, Era.I, false));
        }
    }

    private Card stub(String id, Era era, boolean survives) {
        return new StubCard(id, era, survives);
    }

    private static class StubCard extends Card {
        private final boolean survives;

        StubCard(String id, Era era, boolean survives) {
            super(id, era, 1);
            this.survives = survives;
        }

        @Override public void addToTribe(Player player, Tribe tribe) { }
        @Override public boolean survivesRoundEnd() { return survives; }
    }
}
