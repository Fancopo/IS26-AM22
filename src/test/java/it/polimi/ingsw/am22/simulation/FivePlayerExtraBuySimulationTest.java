package it.polimi.ingsw.am22.simulation;

import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.building.Building;
import it.polimi.ingsw.am22.model.states.ActionResolutionState;
import it.polimi.ingsw.am22.model.states.BonusCardSelectionState;
import it.polimi.ingsw.am22.model.states.TotemPlacementState;

import java.util.ArrayList;
import java.util.List;

public class FivePlayerExtraBuySimulationTest {

    private static final String SPECIAL_ID = "116";
    private static final int MAX_ATTEMPTS = 200;

    public static void main(String[] args) {
        new FivePlayerExtraBuySimulationTest().simulateUntilExtraBuyTriggers();
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }
    private static void assertEquals(int expected, int actual, String msg) {
        if (expected != actual) throw new AssertionError(msg + " (expected " + expected + ", got " + actual + ")");
    }

    void simulateUntilExtraBuyTriggers() {
        Game game = null;
        boolean found = false;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            game = newGame();
            game.startMatch();
            // Inspect the era-III buildings that ended up in the market by
            // peeking after handleEraChange would reveal them. Easier: peek
            // the buildingMarket (era-II + era-III) via reflection.
            if (marketContains(game, SPECIAL_ID)) { found = true; break; }
        }
        assertTrue(found, "Expected building #" + SPECIAL_ID + " to be in market within "
                + MAX_ATTEMPTS + " attempts");

        System.out.println("=== INITIAL SETUP (5 players) ===");
        printPlayers(game);
        printBoard(game);

        // Drive rounds until the special building appears in upper row,
        // then have a player buy it on their turn.
        int safety = 0;
        boolean specialBought = false;
        boolean bonusObserved = false;
        Player buyer = null;
        while (game.getCurrentRound() <= 10 && !game.isGameEnded() && safety++ < 60) {
            int roundAtStart = game.getCurrentRound();
            System.out.println("\n========== ROUND " + roundAtStart
                    + " (Era " + game.getCurrentEra() + ") ==========");
            System.out.println(">> Phase: " + game.getCurrentPhaseName());

            // PHASE: TOTEM PLACEMENT
            assertTrue(game.getCurrentState() instanceof TotemPlacementState,
                    "Expected TotemPlacement at round start, got " + game.getCurrentPhaseName());
            placeAllTotems(game);

            // PHASE: ACTION RESOLUTION
            // Each player resolves in offer-track order
            while (game.getCurrentState() instanceof ActionResolutionState) {
                Player active = game.getActivePlayer();
                List<Card> selection = chooseSelection(game, active, !specialBought);
                String prettySel = selection.isEmpty()
                        ? "(no cards - food tile)"
                        : selection.stream().map(c -> c.getId() + "/" + c.cardDetailType()).toList().toString();
                System.out.println("  CMD " + active.getNickname()
                        + " -> pickCards " + prettySel
                        + "  [food before=" + active.getFood() + "]");
                if (selection.stream().anyMatch(c -> SPECIAL_ID.equals(c.getId()))) {
                    specialBought = true;
                    buyer = active;
                    System.out.println("  *** " + active.getNickname()
                            + " just bought the bonus-draw building (#" + SPECIAL_ID + ") ***");
                }
                game.pickCards(active, selection);
            }

            // OPTIONAL PHASE: BONUS CARD SELECTION
            if (game.getCurrentState() instanceof BonusCardSelectionState) {
                bonusObserved = true;
                Player bp = game.getActivePlayer();
                Card bonus = pickBonusCard(game);
                System.out.println("  >> BONUS PHASE active for " + bp.getNickname()
                        + " (owns building #" + SPECIAL_ID + "). Picking " + bonus.getId()
                        + "/" + bonus.cardDetailType() + " from upper row for free.");
                String bonusId = bonus.getId();
                game.pickBonusCard(bp, bonus);
                // After pickBonusCard the round wraps up (events + refill). The
                // only invariant we can check is: the picked card now lives in
                // the buyer's tribe and is no longer on the board.
                boolean inTribe = bp.getTribe().getMembers().stream().anyMatch(c -> bonusId.equals(c.getId()))
                        || bp.getTribe().getBuildings().stream().anyMatch(b -> bonusId.equals(b.getId()));
                boolean stillOnBoard = game.getBoard().getUpperRow().stream().anyMatch(c -> bonusId.equals(c.getId()))
                        || game.getBoard().getLowerRow().stream().anyMatch(c -> bonusId.equals(c.getId()));
                assertTrue(inTribe, "Bonus card " + bonusId + " should be in " + bp.getNickname() + "'s tribe");
                assertTrue(!stillOnBoard, "Bonus card " + bonusId + " should no longer be on the board");
                System.out.println("  >> " + bp.getNickname() + " gained card " + bonusId
                        + " for free. Card now in tribe: " + inTribe + ". Still on board: " + stillOnBoard);
            }

            System.out.println("\n--- STATUS at end of round " + roundAtStart + " ---");
            printPlayers(game);
            printBoard(game);

            if (specialBought && bonusObserved) {
                System.out.println("\n=== TEST GOAL REACHED: building #" + SPECIAL_ID
                        + " was bought by " + buyer.getNickname()
                        + " AND the bonus pick was granted at round end ===");
                break;
            }
        }

        assertTrue(specialBought, "Expected at least one player to buy building #" + SPECIAL_ID);
        assertTrue(bonusObserved, "Expected BonusCardSelectionState to activate after the buy");
    }

    // ---------- helpers ----------

    private Game newGame() {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Player p = new Player("P" + i);
            p.setTotem(new Totem("color" + i, p));
            players.add(p);
        }
        return new Game(players);
    }

    /** Reflectively check the buildingMarket for the special id. */
    private boolean marketContains(Game game, String id) {
        try {
            var f = Game.class.getDeclaredField("buildingMarket");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<Building> market = (List<Building>) f.get(game);
            return market.stream().anyMatch(b -> id.equals(b.getId()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Smart placement: if the special building is on the upper row and the
     * active player can afford it, send them to a tile that picks from upper
     * (C is the cheapest, takes 1 upper, 0 lower). Otherwise prefer food-rich
     * tiles for low-food players, then the rest.
     */
    private void placeAllTotems(Game game) {
        while (game.getCurrentState() instanceof TotemPlacementState) {
            Player active = game.getActivePlayer();
            boolean specialOnUpper = game.getBoard().getUpperRow().stream()
                    .anyMatch(c -> SPECIAL_ID.equals(c.getId()));
            int specialCost = 9; // BLD_III_07 prints 9 food
            int discount = active.getTribe().getBuilderDiscount();
            boolean canAffordSpecial = active.getFood() >= Math.max(0, specialCost - discount);

            char[] preference;
            if (specialOnUpper && canAffordSpecial) {
                // Buyer route: 1 upper, no lower => tile C is ideal
                preference = new char[]{'C','F','E','G','A','B','D'};
            } else if (active.getFood() < 4) {
                // Hungry: prefer food tiles
                preference = new char[]{'A','C','F','B','D','E','G'};
            } else {
                preference = new char[]{'F','E','G','C','B','D','A'};
            }

            OfferTile target = null;
            for (char want : preference) {
                for (OfferTile t : game.getBoard().getOfferTrack()) {
                    if (t.isAvailable() && t.getLetter() == want) { target = t; break; }
                }
                if (target != null) break;
            }
            if (target == null) {
                for (OfferTile t : game.getBoard().getOfferTrack()) {
                    if (t.isAvailable()) { target = t; break; }
                }
            }
            System.out.println("  CMD " + active.getNickname() + " -> placeTotem on tile "
                    + target.getLetter()
                    + " (upper=" + target.getUpperCardsToTake()
                    + ", lower=" + target.getLowerCardsToTake()
                    + ", food=" + target.getFoodReward() + ")"
                    + (specialOnUpper && canAffordSpecial ? "  [planning to buy #" + SPECIAL_ID + "]" : ""));
            game.placeTotemOnOffer(active, target);
        }
    }

    /**
     * Build a pick selection respecting the tile constraints.
     * If preferSpecial=true and the special building is in the upper row and
     * the player can afford it, include it.
     */
    private List<Card> chooseSelection(Game game, Player player, boolean preferSpecial) {
        OfferTile tile = game.getBoard().getOfferTrack().stream()
                .filter(t -> t.getOccupiedBy() == player.getTotem())
                .findFirst().orElseThrow();
        List<Card> upper = game.getBoard().getUpperRow();
        List<Card> lower = game.getBoard().getLowerRow();

        int needUpper = tile.getUpperCardsToTake();
        int needLower = tile.getLowerCardsToTake();
        if (needUpper == 0 && needLower == 0) return List.of();

        List<Card> selection = new ArrayList<>();

        // Take mandatory characters from each row
        addUpTo(selection, upper, needUpper, c -> c.isPickable() && !c.isOptionalPurchase());
        addUpTo(selection, lower, needLower, c -> c.isPickable() && !c.isOptionalPurchase());

        // If the special building is in the upper row and we can afford it,
        // make sure to include it (replacing a mandatory character if needed)
        if (preferSpecial && needUpper > 0) {
            Card special = upper.stream()
                    .filter(c -> SPECIAL_ID.equals(c.getId()))
                    .filter(c -> !selection.contains(c))
                    .findFirst().orElse(null);
            if (special != null && canAfford(player, (Building) special, selection)) {
                // Remove one non-special character from upper to make room
                int upperPicked = (int) selection.stream().filter(upper::contains).count();
                if (upperPicked >= needUpper) {
                    for (int i = 0; i < selection.size(); i++) {
                        Card c = selection.get(i);
                        if (upper.contains(c) && !SPECIAL_ID.equals(c.getId())) {
                            selection.remove(i);
                            break;
                        }
                    }
                }
                selection.add(special);
            }
        }

        return selection;
    }

    private boolean canAfford(Player player, Building b, List<Card> selectionSoFar) {
        // Cheap heuristic: ignore future builders in selection, just compare to current food
        int discount = player.getTribe().getBuilderDiscount();
        int cost = Math.max(0, b.getFoodPrice() - discount);
        return player.getFood() >= cost;
    }

    private void addUpTo(List<Card> out, List<Card> row, int max, java.util.function.Predicate<Card> ok) {
        int taken = (int) out.stream().filter(row::contains).count();
        for (Card c : row) {
            if (taken >= max) break;
            if (out.contains(c)) continue;
            if (ok.test(c)) { out.add(c); taken++; }
        }
    }

    private Card pickBonusCard(Game game) {
        // Pick the first pickable upper-row card; bonus pick has no food cost
        for (Card c : game.getBoard().getUpperRow()) {
            if (c.isPickable()) return c;
        }
        return game.getBoard().getUpperRow().get(0);
    }

    private void printPlayers(Game game) {
        for (Player p : game.getPlayers()) {
            System.out.println("  " + p.getNickname()
                    + " | food=" + p.getFood()
                    + " | PP=" + p.getPP()
                    + " | tribe chars=" + p.getTribe().getMembers().size()
                    + " (" + describeChars(p) + ")"
                    + " | buildings=" + p.getTribe().getBuildings().size()
                    + " (" + describeBuildings(p) + ")"
                    + " | hasExtraBuy=" + p.hasExtraBuyAtRoundEnd());
        }
    }

    private String describeChars(Player p) {
        return String.join(",", p.getTribe().getMembers().stream()
                .map(c -> c.getId() + ":" + c.cardDetailType()).toList());
    }
    private String describeBuildings(Player p) {
        return String.join(",", p.getTribe().getBuildings().stream()
                .map(b -> "#" + b.getId() + (b.grantsExtraBuyAtRoundEnd() ? "[+1pick]" : ""))
                .toList());
    }

    private void printBoard(Game game) {
        System.out.println("  upperRow(" + game.getBoard().getUpperRow().size() + "): "
                + game.getBoard().getUpperRow().stream()
                    .map(c -> c.getId() + "/" + c.cardDetailType()).toList());
        System.out.println("  lowerRow(" + game.getBoard().getLowerRow().size() + "): "
                + game.getBoard().getLowerRow().stream()
                    .map(c -> c.getId() + "/" + c.cardDetailType()).toList());
    }
}
