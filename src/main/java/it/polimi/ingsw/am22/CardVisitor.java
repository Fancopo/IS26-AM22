package it.polimi.ingsw.am22;

public interface CardVisitor {
    void visit(Event event);
    void visit(Building building);
    void visit(TribeCharacter character); // Sostituisci con "Character" se hai tenuto quel nome
}