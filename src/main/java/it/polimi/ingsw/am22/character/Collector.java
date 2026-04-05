package il.polimi.ingsw.am22.character;

public class Collector extends Card implements CharacterEffect {

    public Collector(char id, String type, int era, int minPlayers, String characterType){
        super(id, type, era, minPlayers, "Collector");
    }

    @Override
    public void addCharacter(Player player, Tribe tribe) {
        // La carta si aggiunge fisicamente alla collezione della tribù
        player.getTribe().getMembers().add(this);
        System.out.println("Raccoglitore aggiunto. Fornirà uno sconto di 3 cibo durante il Sostentamento.");
    }

    @Override
    public void applyImmediateEffect(Player player, Tribe tribe) {
    }

}
