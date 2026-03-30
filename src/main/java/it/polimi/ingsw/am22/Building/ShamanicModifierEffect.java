package Building;

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
    public void modifyShamanicRitual() {
        // The game engine will fetch these flags when resolving the event
    }

    // Getters for the engine to read
    public int getExtraIcons() { return extraIcons; }
    public boolean isPreventPPLoss() { return preventPPLoss; }
    public boolean isDoubleWinPP() { return doubleWinPP; }
}