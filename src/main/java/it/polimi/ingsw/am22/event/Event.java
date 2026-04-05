package il.polimi.ingse.event;
import il.polimi.ingse.Era;
import il.polimi.ingse.event.EventType;
import java.util.ArrayList;
import java.util.List;

public class Event extends Card {
    private EventType eventType;
    private EventEffect effect;

    public Event(Era era, int minPlayers, EventType eventType, EventEffect effect) {
        super(era, minPlayers);
        this.eventType = eventType;
        this.effect = effect;
    }

    public EventType getEventType() {
        return eventType;
    }
    public abstract void applyEvent(List<Player> players, char id);

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void onRoundEndTrigger(Game game){
        this.effect.applyEvent(game.getPlayers());
    }

    @Override
    public int getTriggerPriority(){
        return this.eventType == EventType.SUSTENANCE ? 1 : 0;
    }

    @Override
    public void addToTribe(Player player, Tribe tribe) {
        throw new UnsupportedOperationException("An event cannot be added to the tribe");
    }
}



