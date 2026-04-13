package it.polimi.ingsw.am22.model.event;
import it.polimi.ingsw.am22.model.Player;

import java.util.List;
public interface EventEffect {
    void applyEvent(List<Player> players, String id);
}
