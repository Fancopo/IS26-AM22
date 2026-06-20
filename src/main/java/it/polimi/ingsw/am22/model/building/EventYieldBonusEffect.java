package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.event.EventType;

/**
 * {@link BuildingEffect} that grants the owner extra food and/or PP while a
 * specific event resolves, scaled by a relevant character count.
 */
public class EventYieldBonusEffect implements BuildingEffect {
    private final EventType targetEventType;
    private final int bonusFood;
    private final int bonusPP;

    /**
     * @param targetEventType the event during which the bonus applies
     * @param bonusFood       food granted per matching character
     * @param bonusPP         PP granted per matching character
     */
    public EventYieldBonusEffect(EventType targetEventType, int bonusFood, int bonusPP) {
        this.targetEventType = targetEventType;
        this.bonusFood = bonusFood;
        this.bonusPP = bonusPP;
    }

    /**
     * Applies the bonus if the resolving event matches the target type.
     *
     * @param eventType      the event being resolved
     * @param player         the building's owner
     * @param characterCount the count the bonus is scaled by
     */
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
