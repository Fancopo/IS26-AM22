package it.polimi.ingsw.am22.character;

import it.polimi.ingsw.am22.*;
import it.polimi.ingsw.am22.Building.Building;

public abstract class TribeCharacter extends Card {

    // Attributi definiti nel diagramma UML
    private CharacterType characterType;

    // Costruttore
    public TribeCharacter(String id, Era era, int minPlayers, CharacterType characterType) {
        super(id, era, minPlayers);
        this.characterType = characterType;
    }
    public CharacterType getCharacterType() { return characterType; }


    public void addToTribe(Player player, Tribe tribe) {
        // Esattamente come il cacciatore: si aggiunge alla tribù
        player.getTribe().getMembers().add(this);
        for (Building b : player.getTribe().getBuildings()) {
            if (b.getEffect() != null) {
                b.getEffect().onCharacterAdded(player, this);
            }
        }
    }
    public int getNumStars(){return 0;}

    public char getIconPerInventor(){return '0';}
    public int getDiscountFood(){
        return 0;
    }public int getPP(){
        return 0;
    }

}