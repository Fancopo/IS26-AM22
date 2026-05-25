package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.event.EventType;

public class EventYieldBonusEffect implements BuildingEffect {
    private final EventType targetEventType;
    private final int bonusFood;
    private final int bonusPP;

    public EventYieldBonusEffect(EventType targetEventType, int bonusFood, int bonusPP) {
        this.targetEventType = targetEventType;
        this.bonusFood = bonusFood;
        this.bonusPP = bonusPP;
    }

    @Override
    public void applyEventBonus(EventType eventType, Player player, int characterCount) {
        if (this.targetEventType != eventType) return;

        int extraFood = bonusFood * characterCount;
        int extraPP = bonusPP * characterCount;

        if (extraFood > 0) player.addFood(extraFood);
        if (extraPP > 0) player.addPP(extraPP);
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("Event yield bonus during ")
                .append(targetEventType).append(":");
        if (bonusFood > 0) sb.append(" +").append(bonusFood).append(" food per matching character;");
        if (bonusPP > 0)   sb.append(" +").append(bonusPP).append(" PP per matching character;");
        return sb.toString();
    }
}
