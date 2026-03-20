package it.polimi.ingsw.am22;

public abstract class Building extends Card {
    private int foodPrice;
    private int finalPP;
    private boolean currentApplicable;

    public Building(char id, Era era, int minPlayers, int foodPrice, int finalPP) {
        // As per the UML, Building extends Card. Type is inherently "Building".
        super(id, "Building", era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
        this.currentApplicable = true; // Default state
    }

    public int getFoodPrice() {
        return foodPrice;
    }

    public int getFinalPP() {
        return finalPP;
    }

    public boolean isCurrentApplicable() {
        return currentApplicable;
    }

    public void setCurrentApplicable(boolean currentApplicable) {
        this.currentApplicable = currentApplicable;
    }

    // Reduces the food cost based on the Builders the Player has in their Tribe
    public void discount(Player player) {
        // Implementation would check player.getTribe().getMembers() for Builders
        // and reduce this.foodPrice accordingly, ensuring it doesn't drop below 0.
    }

    // Abstract method to be overridden by specific buildings
    public abstract void ApplyEffect();
}



