package it.polimi.ingsw.am22.model.building;

/**
 * Discriminator used in the building JSON to choose which {@link BuildingEffect}
 * implementation to instantiate when loading a building.
 */
public enum BuildingEffectType {
    /** Awards victory points at the end of the game ({@link EndGameScoringEffect}). */
    ENDGAME_SCORING_EFFECT,
    /** Modifies the Shaman scoring ({@link ShamanicModifierEffect}). */
    SHAMANIC_MODIFIER_EFFECT,
    /** Feeds extra characters during the Sustenance event ({@link SustenanceDiscountEffect}). */
    SUSTENANCE_DISCOUNT_EFFECT,
    /** Rewards food for collecting character sets ({@link CollectionRewardEffect}). */
    COLLECTION_REWARD_EFFECT,
    /** Grants a bonus while a specific event resolves ({@link EventYieldBonusEffect}). */
    EVENT_YIELD_BONUS_EFFECT,
    /** Alters turn-phase rewards ({@link TurnPhaseModifierEffect}). */
    TURN_PHASE_MODIFIER_EFFECT
}
