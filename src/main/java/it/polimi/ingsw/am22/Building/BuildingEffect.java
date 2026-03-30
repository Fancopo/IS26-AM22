package Building;

public enum Era { I, II, III }
public enum CharacterType { INVENTOR, BUILDER, GATHERER, SHAMAN, ARTIST, HUNTER }
public enum EventType { SUSTENANCE, HUNTING, SHAMANIC_RITUAL, CAVE_PAINTINGS }
public enum CollectionCondition { SET_OF_6, INVENTOR_PAIR }

public interface BuildingEffect {
    default int calculateEndGame(Tribe tribe) { return 0; }
    default void modifyShamanicRitual() {}
    default void onTotemPlaced() {}
    default void onRoundEnd() {}
    default int getSustenanceDiscount(Tribe tribe) { return 0; }
    default void onCharacterAdded(Player player, TribeCharacter newChar) {}
    default void modifyEventYield() {}

