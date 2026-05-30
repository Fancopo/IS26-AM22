package it.polimi.ingsw.am22.model.building;

import it.polimi.ingsw.am22.model.Era;
import it.polimi.ingsw.am22.model.PickSimulation;
import it.polimi.ingsw.am22.model.Player;
import it.polimi.ingsw.am22.model.Tribe;
import it.polimi.ingsw.am22.model.Card;

public class Building extends Card {
    private final int foodPrice;
    private final int finalPP;
    private final BuildingEffect effect;

    public Building(String id, Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect) {
        super(id, era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
        this.effect = effect;
    }

    public static int FinalBuildingPP(Tribe tribe) {
        int total = 0;
        for (Building b : tribe.getBuildings()) {
            total += b.getFinalPP();
            total += b.getEffect().calculateEndGame(tribe);
        }
        return total;
    }

    @Override
    public void addToTribe(Player player, Tribe tribe) {
        tribe.addBuilding(this);
    }

    @Override
    public boolean survivesRoundEnd() {
        return true;
    }

    @Override
    public boolean isDestroyedOnEraIII() {
        return true;
    }

    public boolean grantsExtraBuyAtRoundEnd() {
        return this.effect != null && this.effect.hasExtraBuyAtRoundEnd();
    }

    @Override
    public boolean isOptionalPurchase() { return true; }

    @Override
    public String cardCategory() { return "BUILDING"; }

    @Override
    public String cardDetailType() { return "BUILDING"; }

    @Override
    public int getFoodCost() { return foodPrice; }

    /** Validation: deduct the discounted cost from the simulated food (throws if insufficient). */
    @Override
    public void applyPickEffect(PickSimulation sim) {
        int cost = Math.max(0, foodPrice - sim.getBuilderDiscount());
        sim.payFood(cost);
    }

    /** Commit: pay the discounted cost using the tribe's *current* discount. */
    @Override
    public void payPickCost(Player player) {
        int discount = player.getTribe().getBuilderDiscount();
        int cost = Math.max(0, foodPrice - discount);
        player.payFood(cost);
    }

    public int getFoodPrice() { return foodPrice; }
    public int getFinalPP() { return finalPP; }
    public BuildingEffect getEffect() { return effect; }

    /** Called when the owner's totem lands on a food slot of the turn-order tile. */
    public void applyOnFoodSlotPlaced(Player player) {
        if (this.effect != null) {
            this.effect.onTotemPlaced(player);
        }
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("Building: costs ")
                .append(foodPrice).append(" food (before Builder discount), worth ")
                .append(finalPP).append(" PP at end of game.");
        if (effect != null) {
            String effectText = effect.describe();
            if (effectText != null && !effectText.isEmpty()) {
                sb.append(' ').append(effectText);
            }
        }
        return sb.toString();
    }
}
