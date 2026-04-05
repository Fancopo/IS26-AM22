package it.polimi.ingse.character;

import javafx.application.Application;

public class TribeCharacter extends Card {

    // Attributi definiti nel diagramma UML
    private CharacterType characterType;

    // Costruttore
    public TribeCharacter(Era era, int minPlayers, CharacterType characterType,) {
        super(era, minPlayers);
        this.characterType = characterType;
    }
    public CharacterType getCharacterType() { return characterType; }

    @Override
    public void accept(CardVisitor visitor) {
        visitor.visit(this);
    }

    public int getProvidedIcons() {
        return 0;
    }

    @Override
    public void addToTribe(Player player, Tribe tribe) {
        // Esattamente come il cacciatore: si aggiunge alla tribù
        player.getTribe().getMembers().add(this);
        for (Building b : player.getTribe().getBuildings()) {
            if (b.getEffect() != null) {
                b.getEffect().onCharacterAdded(player, this.getProvidedIcons());
            }
        }
    }
}