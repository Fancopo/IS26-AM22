package it.polimi.ingsw.am22.model.Building;


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
        // Se l'edificio ha questo flag attivo, dà il bonus(+1 cibo)
        // Viene chiamato dal GameState solo quando il totem va su uno slot cibo
        if(extraFoodOnTurnOrderBonus){
        owner.addFood(1);
        }
    }

    @Override
    public boolean hasExtraBuyAtRoundEnd(){
        // Espone il flag alla macchina a stati
        return this.extraBuyAtRoundEnd;
    }
}