package Building;

import il.polimi.ingse.event.EventType

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
    public void applyEventBonus(EventType eventType, Player player, int characterCount) {
        if (this.targetEventType == eventType) {

            // Multiply the bonus by the amount of characters (Hunters, Artists)
            int extraFood = this.bonusFood * characterCount;
            int extraPP = this.bonusPP * characterCount;

            if (extraFood > 0) {
                player.addFood(extraFood);
                System.out.println("Building Bonus: " + player.getNickname() + " got +" + extraFood + " Food!");
            }
            if (extraPP > 0) {
                player.addPP(extraPP);
                System.out.println("Building Bonus: " + player.getNickname() + " got +" + extraPP + " PP!");
            }
        }
    }
}