package Building;

public interface BuildingEffect {
    default int calculateEndGame(Tribe tribe) { return 0; }
    default void modifyShamanicRitual() {}
    default void onTotemPlaced() {}
    default void onRoundEnd() {}
    default int getSustenanceDiscount(Tribe tribe) { return 0; }
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}
    default void modifyEventYield() {}

