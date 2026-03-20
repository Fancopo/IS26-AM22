package il.polimi.ingse.event;
import java.util.ArrayList;
import java.util.List;

public class Event extends Card{


    public Event(char id, String type, int era, int minPlayers){
        super(id, type, era, minPlayers);
    }
    public abstract void applyEvent(List<Player> players, char id);

}
