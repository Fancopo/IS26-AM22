package it.polimi.ingsw.am22.model.character;


/**
 * Polymorphic attributes that each {@link TribeCharacter} subclass exposes.
 * Default values are provided by {@link TribeCharacter} so callers can read
 * them uniformly without type-checking the concrete subclass.
 */
public interface CharacterEffect extends java.io.Serializable {
    /** @return the number of Shaman star icons (0 for non-Shamans) */
    int getNumStars();

    /** @return the Inventor icon ({@code '0'} for non-Inventors) */
    char getIconPerInventor();

    /** @return the Builder food discount (0 for non-Builders) */
    int getDiscountFood();

    /** @return the printed end-game victory points (0 unless the card prints any) */
    int getPP();
}
