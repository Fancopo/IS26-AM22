package it.polimi.ingsw.am22.model.building;


import it.polimi.ingsw.am22.model.Player;

// 6. TurnPhaseModifierEffect
public class TurnPhaseModifierEffect implements BuildingEffect {
    private boolean extraFoodOnTurnOrderBonus;
    private boolean extraBuyAtRoundEnd;

    public TurnPhaseModifierEffect(boolean extraFoodOnTurnOrderBonus, boolean extraBuyAtRoundEnd) {
        this.extraFoodOnTurnOrderBonus = extraFoodOnTurnOrderBonus;
        this.extraBuyAtRoundEnd = extraBuyAtRoundEnd;
    }

    @Override
    public void onTotemPlaced(Player owner){
        // If this flag is active on the building, grant the bonus (+1 food)
        // Called by GameState only when the totem lands on a food slot
        if(extraFoodOnTurnOrderBonus){
        owner.addFood(1);
        }
    }

    @Override
    public boolean hasExtraBuyAtRoundEnd(){
        // Expose the flag to the state machine
        return this.extraBuyAtRoundEnd;
    }
}
