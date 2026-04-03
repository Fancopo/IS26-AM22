package it.polimi.ingsw.am22.event;
import java.util.List;
public interface EventEffect {
    void applyEvent(List<Player> players, char id);
}
