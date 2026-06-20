package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.Player;

import java.util.List;

/**
 * Strategy applied when an {@link Event} resolves at the end of a round.
 */
public interface EventEffect extends java.io.Serializable {
    /**
     * Applies the event to every player.
     *
     * @param players the players in the game
     * @param id      the id of the resolving event
     */
    void applyEvent(List<Player> players, String id);
}
