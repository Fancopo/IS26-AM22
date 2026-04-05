package it.polimi.ingsw.am22.event;
import it.polimi.ingsw.am22.BuildingEffect;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.character.CharacterType;

import java.util.ArrayList;
import java.util.List;


public class hunting extends Event implements EventEffect{

    public Hunting(Era era, int minPlayers) {
        super(era, minPlayers, EventType.HUNTING);
    }

    @Override
    public void applyEvent(List<Player> players, char id) {
        int PPperHunter = 0;

        // Valori di Punti Prestigio per cacciatore basati sull'Era corrente (dal tuo snippet)
        if (this.era == Era.I) {
            PPperHunter = 1;
        } else if (this.era == Era.II) {
            PPperHunter = 2;
        } else if (this.era == Era.III) {
            PPperHunter = 3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // 1. Conta quanti Cacciatori ci sono nella tribù
            int huntersCount = tribe.countCharacters(CharacterType.HUNTER);

            if (huntersCount > 0) {
                // Valori base forniti dall'evento per ogni Cacciatore
                int foodPerHunter = 1;
                for (Building building : player.getTrbe().getBuildings()){
                    building.getEffect().applyEventBonus(EventType.HUNTING,player,huntersCount);
                }


                // 3. Calcolo dei totali
                int totalFoodToAdd = huntersCount * (foodPerHunter);
                int totalPPToAdd = huntersCount * (PPperHunter);

                // 4. Assegnazione delle risorse al giocatore
                player.addFood(totalFoodToAdd);
                player.addPP(totalPPToAdd);

                System.out.println(player.getNickname() + " ha " + huntersCount +
                        " Cacciatori. Ottiene " + totalFoodToAdd +
                        " Cibo e " + totalPPToAdd + " PP dalla Caccia!");
            } else {
                System.out.println(player.getNickname() + " non ha Cacciatori e non ottiene nulla.");
            }
        }
    }

}
