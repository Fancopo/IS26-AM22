package it.polimi.ingsw.am22.Building;


import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.Card;
import it.polimi.ingsw.am22.Building.BuildingEffect;

public class Building extends Card {
    private int foodPrice;
    private int finalPP;
    private BuildingEffect effect;

    public Building(String id, Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect) {
        super(id, era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
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
    public void addToTribe(Player player, Tribe tribe) {
        // La carta aggiunge se stessa alla lista degli edifici della tribù
        tribe.addBuilding(this);
    }


    // Gli Edifici sopravvivono al reset di fine round
    @Override
    public boolean survivesRoundEnd() {
        return true;
    }

    // Gli Edifici nella riga inferiore vengono distrutti all'Era III
    @Override
    public boolean isDestroyedOnEraIII() {
        return true;
    }

    public void applyOnTotemPlaced(Player owner) {
        if (this.effect != null) {
            this.effect.onTotemPlaced(owner);
        }
    }

    // Controlla se questo specifico edificio fornisce l'acquisto extra.
    public boolean grantsExtraBuyAtRoundEnd() {
        return this.effect != null && this.effect.hasExtraBuyAtRoundEnd();
    }

    public int getFoodPrice() { return foodPrice; }
    public int getFinalPP() { return finalPP; }
    public BuildingEffect getEffect() { return effect; }
    public void applyOnFoodSlotPlaced(Player player) {}
}