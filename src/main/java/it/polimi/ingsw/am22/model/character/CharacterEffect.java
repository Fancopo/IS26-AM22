package it.polimi.ingsw.am22.model.character;


/**
 * Polymorphic attributes that each {@link TribeCharacter} subclass exposes.
 * Default values are provided by {@link TribeCharacter} so callers can read
 * them uniformly without type-checking the concrete subclass.
 */
public interface CharacterEffect extends java.io.Serializable {
    int getNumStars();
    char getIconPerInventor();
    int getDiscountFood();
    int getPP();
}
