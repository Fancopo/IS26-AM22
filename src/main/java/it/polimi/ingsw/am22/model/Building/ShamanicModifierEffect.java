package it.polimi.ingsw.am22.model.Building;

// 2. ShamanicModifierEffect
public class ShamanicModifierEffect implements BuildingEffect {
    private int extraIcons;
    private boolean preventPPLoss;
    private boolean doubleWinPP;

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
}