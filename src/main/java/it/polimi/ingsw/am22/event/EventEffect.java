package it.polimi.ingsw.am22.event;
import it.polimi.ingsw.am22.Player;

import java.util.List;
public interface EventEffect {
    void applyEvent(List<Player> players, String id);
}
