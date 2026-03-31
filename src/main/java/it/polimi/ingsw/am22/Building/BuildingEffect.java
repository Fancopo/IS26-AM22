package Building;

public interface BuildingEffect {
    default int calculateEndGame(Tribe tribe) { return 0; }
    default int getExtraShamanIcons() {return 0;}
    default boolean preventsShamanPPLoss() {return false;}
    default boolean doublesShamanWinPP() {return false;}
    default void onTotemPlaced() {}
    default void onRoundEnd() {}
    default int getSustenanceDiscount(Tribe tribe) { return 0; }
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}
    default void applyEventBonus(EventType eventType, Player player, int characterCount) {}
