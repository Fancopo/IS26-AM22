package il.polimi.ingse.event;
import java.util.List;
public interface EventEffect {
    void applyEvent(List<Player> players, char id);
}
