package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.Card;
import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.Game;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EventResolutionStateTest {

    private TestGame game;
    private List<String> triggerLog;
    private final EventResolutionState state = new EventResolutionState();

    @BeforeEach
    void setUp() {
        game = new TestGame(List.of(new Player("P1"), new Player("P2")));
        triggerLog = new ArrayList<>();
    }

    @Test
    void triggersLowerRowCardsInPriorityOrder() {
        keepDeckNonEmpty();
        game.getBoard().getLowerRow().add(recording("deferred", 1));
        game.getBoard().getLowerRow().add(recording("immediate", 0));

        state.resolveEvents(game);

        assertEquals(List.of("immediate", "deferred"), triggerLog,
                "Lower priority value resolves first");
    }

    @Test
    void transitionsToRoundUpdateAndDrivesIt() {
        keepDeckNonEmpty();

        state.resolveEvents(game);

        assertInstanceOf(RoundUpdateState.class, game.getCurrentState());
        assertEquals(1, game.updateRoundCalls, "Event resolution must hand off to the round update");
    }

    @Test
    void resolvesUpperRowEventsOnTheFinalRound() {
        keepDeckNonEmpty();
        game.setCurrentRound(10);
        game.getBoard().getUpperRow().add(recordingEvent("finalEvent", 0));
        game.getBoard().getUpperRow().add(recording("notAnEvent", 0)); // not an event -> ignored

        state.resolveEvents(game);

        assertEquals(List.of("finalEvent"), triggerLog);
    }

    @Test
    void resolvesUpperRowEventsWhenTheDeckIsExhausted() {
        // deck left empty (the default) signals the last round
        game.getBoard().getUpperRow().add(recordingEvent("finalEvent", 0));

        state.resolveEvents(game);

        assertEquals(List.of("finalEvent"), triggerLog);
    }

    @Test
    void leavesUpperRowEventsUntouchedMidGame() {
        keepDeckNonEmpty();
        game.setCurrentRound(5);
        game.getBoard().getUpperRow().add(recordingEvent("upperEvent", 0));

        state.resolveEvents(game);

        assertTrue(triggerLog.isEmpty(), "Upper-row events only fire on the final round");
    }

    @Test
    void exposesPhaseName() {
        assertEquals("Event Resolution", state.getPhaseName());
    }

    // ==================== helpers ====================

    /** Adds a throwaway card so {@code tribeDeck.isEmpty()} is false (not the last round). */
    private void keepDeckNonEmpty() {
        game.getTribeDeck().add(recording("deck-filler", 0));
    }

    private Card recording(String id, int priority) {
        return new RecordingCard(id, triggerLog, priority, false);
    }

    private Card recordingEvent(String id, int priority) {
        return new RecordingCard(id, triggerLog, priority, true);
    }

    /** Game double that records the hand-off to the round update instead of running it. */
    private static class TestGame extends Game {
        int updateRoundCalls = 0;

        TestGame(List<Player> players) { super(players); }

        @Override
        public void updateRound() { updateRoundCalls++; }
    }

    /** Card that appends its id to a shared log when triggered at round end. */
    private static class RecordingCard extends Card {
        private final List<String> log;
        private final int priority;
        private final boolean event;

        RecordingCard(String id, List<String> log, int priority, boolean event) {
            super(id, Era.I, 1);
            this.log = log;
            this.priority = priority;
            this.event = event;
        }

        @Override public void addToTribe(Player player, Tribe tribe) { }
        @Override public void onRoundEndTrigger(Game game) { log.add(getId()); }
        @Override public int getTriggerPriority() { return priority; }
        @Override public boolean isEvent() { return event; }
    }
}
