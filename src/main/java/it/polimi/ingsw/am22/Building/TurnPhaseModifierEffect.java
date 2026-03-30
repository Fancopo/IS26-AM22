package Building;

// 6. TurnPhaseModifierEffect
public class TurnPhaseModifierEffect implements BuildingEffect {
    private boolean extraFoodOnTurnOrderBonus;
    private boolean extraBuyAtRoundEnd;

    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

    @Override
    public void onTotemPlaced() {
        // Logic executed by the Game class when a totem is placed
    }

    @Override
    public void onRoundEnd() {
        // Logic executed by the Game class during the cleanup phase
    }

    public boolean isExtraFoodOnTurnOrderBonus() { return extraFoodOnTurnOrderBonus; }
    public boolean isExtraBuyAtRoundEnd() { return extraBuyAtRoundEnd; }
}