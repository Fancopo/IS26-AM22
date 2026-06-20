package it.polimi.ingsw.am22.model.building;

/**
 * {@link BuildingEffect} that modifies how the Shaman scoring treats the owner:
 * it can add star icons, shield them from the PP loss when they hold the fewest
 * stars, and/or double the PP they gain when they hold the most.
 */
public class ShamanicModifierEffect implements BuildingEffect {
    private final int extraIcons;
    private final boolean preventPPLoss;
    private final boolean doubleWinPP;

    /**
     * @param extraIcons    extra star icons granted to the owner
     * @param preventPPLoss whether to shield the owner from the "fewest stars" PP loss
     * @param doubleWinPP   whether to double the PP gained for holding the most stars
     */
    public ShamanicModifierEffect(int extraIcons, boolean preventPPLoss, boolean doubleWinPP) {
        this.extraIcons = extraIcons;
        this.preventPPLoss = preventPPLoss;
        this.doubleWinPP = doubleWinPP;
    }

    @Override
    public int getExtraShamanIcons() { return extraIcons; }

    @Override
    public boolean preventsShamanPPLoss() { return preventPPLoss; }

    @Override
    public boolean doublesShamanWinPP() { return doubleWinPP; }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder("Shamanic Ritual:");
        if (extraIcons > 0) sb.append(" +").append(extraIcons).append(" star icon(s);");
        if (preventPPLoss)  sb.append(" prevents PP loss when you have the fewest stars;");
        if (doubleWinPP)    sb.append(" doubles the PP gained when you have the most stars;");
        return sb.toString();
    }
}
