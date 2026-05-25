package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Player;

public class TurnPhaseModifierEffect implements BuildingEffect {
    private boolean extraFoodOnTurnOrderBonus;
    private boolean extraBuyAtRoundEnd;

    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

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
