package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Totem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: {@code startMatch} loads the real card decks from the
 * classpath resources, exactly as the running game does.
 */
class SetUpStateTest {

    private static final int PLAYERS = 3;

    private List<Player> players;
    private Game game;

    @BeforeEach
    void setUp() {
        players = new ArrayList<>();
        for (int i = 1; i <= PLAYERS; i++) {
            Player p = new Player("P" + i);
            p.setTotem(new Totem("color" + i, p));
            players.add(p);
        }
        game = new Game(players); // a fresh Game starts in SetUpState
    }

    @Test
    void freshStateAdvertisesItselfAsSetup() {
        assertEquals("Initial Setup", new SetUpState().getPhaseName());
        assertTrue(new SetUpState().isSetupPhase());
        assertFalse(game.isGameStarted(), "Game has not started before startMatch");
    }

    @Test
    void startMatchHandsOverToTotemPlacement() {
        game.startMatch();

        assertInstanceOf(TotemPlacementState.class, game.getCurrentState());
        assertTrue(game.isGameStarted());
    }

    @Test
    void startMatchBuildsTheTurnOrderTileAndOfferTrack() {
        game.startMatch();

        assertEquals(PLAYERS, game.getBoard().getTurnOrderTile().getSlots().size());
        assertEquals(5, game.getBoard().getOfferTrack().size(), "3 players use a 5-tile offer track");
    }

    @Test
    void startMatchSeatsEveryTotemInTheTurnOrder() {
        game.startMatch();

        for (Player p : game.getPlayers()) {
            assertNotNull(p.getTotem().getCurrentSlot(), p.getNickname() + " should be seated");
        }
        assertEquals(PLAYERS, game.getBoard().getTurnOrderTile().getOccupiedSlotsCount());
    }

    @Test
    void startMatchDealsTheOpeningRows() {
        game.startMatch();

        assertEquals(PLAYERS + 1, game.getBoard().getLowerRow().size(),
                "Lower row holds numPlayers + 1 cards");
        assertTrue(game.getBoard().getUpperRow().size() >= PLAYERS + 4,
                "Upper row is topped up to at least numPlayers + 4");
        assertFalse(game.getTribeDeck().isEmpty(), "Cards remain in the deck after dealing");
    }

    @Test
    void startMatchDistributesStartingFood() {
        game.startMatch();

        int totalFood = game.getPlayers().stream().mapToInt(Player::getFood).sum();
        assertEquals(8, totalFood, "Starting food for 3 players is 2 + 3 + 3");

        assertNotNull(game.getActivePlayer());
        assertEquals(2, game.getActivePlayer().getFood(),
                "The first player in turn order starts with 2 food");
    }
}
