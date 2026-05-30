package it.polimi.ingsw.am22.model.building;

public class ShamanicModifierEffect implements BuildingEffect {
    private final int extraIcons;
    private final boolean preventPPLoss;
    private final boolean doubleWinPP;

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
