package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.OfferTile;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Totem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TotemPlacementStateTest {

    private List<Player> players;

    @Test
    void placingABonusBeforeTheLastPlayerAdvancesTurnWithoutChangingState() {
        Game game = newGameInPlacement(3);

        game.placeTotemOnOffer(players.get(0), tile(game, 'B'));

        assertSame(players.get(0).getTotem(), tile(game, 'B').getOccupiedBy());
        assertInstanceOf(TotemPlacementState.class, game.getCurrentState(),
                "Still placing totems until everyone has placed");
        assertSame(players.get(1), game.getActivePlayer(), "Turn passes to the next player");
    }

    @Test
    void lastPlacementSwitchesToActionResolutionAndActivatesLeftmostTotem() {
        Game game = newGameInPlacement(3);

        game.placeTotemOnOffer(players.get(0), tile(game, 'C'));
        game.placeTotemOnOffer(players.get(1), tile(game, 'B')); // leftmost letter
        game.placeTotemOnOffer(players.get(2), tile(game, 'D'));

        assertInstanceOf(ActionResolutionState.class, game.getCurrentState());
        assertSame(players.get(1), game.getActivePlayer(),
                "The player on the leftmost occupied tile (B) acts first");
    }

    @Test
    void exposesPhaseName() {
        assertEquals("Totem Placement", new TotemPlacementState().getPhaseName());
    }

    // ==================== helpers ====================

    private Game newGameInPlacement(int playerCount) {
        players = new ArrayList<>();
        for (int i = 1; i <= playerCount; i++) {
            Player p = new Player("P" + i);
            p.setTotem(new Totem("color" + i, p));
            players.add(p);
        }
        Game game = new Game(players);
        game.getBoard().initTrack(playerCount);
        game.setState(new TotemPlacementState());
        game.setActivePlayer(players.get(0));
        return game;
    }

    private OfferTile tile(Game game, char letter) {
        return game.getBoard().getOfferTrack().stream()
                .filter(t -> t.getLetter() == letter)
                .findFirst().orElseThrow();
    }
}
