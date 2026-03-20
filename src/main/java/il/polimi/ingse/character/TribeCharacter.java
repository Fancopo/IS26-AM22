package il.polimi.ingse.character;

import javafx.application.Application;

public class TribeCharacter extends Card{
    private String characterType;

    public TribeCharacter(char id, String type, int era, int minPlayers, String characterType,){

        super(id, type, era, minPlayers);
        this.characterType = characterType;

    }

    public String getCharacterType(){
        return characterType;
    }

    public abstract void applyEffect(Player player, Tribe tribe);
    }
}
