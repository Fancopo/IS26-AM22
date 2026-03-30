package Building;

// 5. EventYieldBonusEffect
public class EventYieldBonusEffect implements BuildingEffect {
    private EventType targetEventType;
    private int bonusFood;
    private int bonusPP;

    public EventYieldBonusEffect(EventType targetEventType, int bonusFood, int bonusPP) {
        this.targetEventType = targetEventType;
        this.bonusFood = bonusFood;
        this.bonusPP = bonusPP;
    }

    @Override
    public void modifyEventYield() {
        // The game engine will fetch these bonuses when the specific event occurs
    }

    public EventType getTargetEventType() { return targetEventType; }
    public int getBonusFood() { return bonusFood; }
    public int getBonusPP() { return bonusPP; }
}