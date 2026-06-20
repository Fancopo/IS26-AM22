package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Player;

/**
 * {@link BuildingEffect} that tweaks turn-phase rewards: it can grant extra food
 * whenever the owner's totem lands on a food slot, and/or grant an extra Building
 * purchase at the end of the round.
 */
public class TurnPhaseModifierEffect implements BuildingEffect {
    private final boolean extraFoodOnTurnOrderBonus;
    private final boolean extraBuyAtRoundEnd;

    /**
     * @param extraFoodOnTurnOrderBonus whether to grant +1 food on each food-slot landing
     * @param extraBuyAtRoundEnd        whether to grant an extra buy at round end
     */
    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

    /**
     * Grants +1 food when configured to do so.
     *
     * @param owner the building's owner
     */
    @Override
    public void onTotemPlaced(Player owner) {
        if (extraFoodOnTurnOrderBonus) {
            owner.addFood(1);
        }
    }

    @Override
    public boolean hasExtraBuyAtRoundEnd() {
        return extraBuyAtRoundEnd;
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("Turn-phase modifier:");
        if (extraFoodOnTurnOrderBonus) sb.append(" +1 food whenever your totem lands on a food slot;");
        if (extraBuyAtRoundEnd)        sb.append(" grants an extra Building purchase at round end;");
        return sb.toString();
    }
}
