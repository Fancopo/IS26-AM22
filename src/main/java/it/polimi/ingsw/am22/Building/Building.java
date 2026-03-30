package Building;

import java.util.HashMap;
import java.util.Map;

public class Building extends Card {
    private int foodPrice;
    private int finalPP;
    private boolean currentApplicable;
    private BuildingEffect effect;

    public Building(String id, Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect) {
        super(id, era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
        this.currentApplicable = true;
        this.effect = effect;
    }

    // Sum base PP + Effect PP
    public static int FinalBuildingPP(Tribe tribe) {
        int totalSum = 0;

        for (Building b : tribe.getBuildings()) {

            // 1. Add the flat points printed on the building card
            totalSum += b.getFinalPP();

            // 2. Add the bonus points from the effect.
            totalSum += b.getEffect().calculateEndGame(tribe);

        }

        return totalSum;
    }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }

    public void ApplyEffect() {
    }

    public int getFoodPrice() { return foodPrice; }
    public int getFinalPP() { return finalPP; }
    public boolean isCurrentApplicable() { return currentApplicable; }
    public BuildingEffect getEffect() { return effect; }
}