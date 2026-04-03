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
                int extraPPPerHunter = 0;
                int extraFoodPerHunter = 0;

                // 2. Controlla gli effetti degli Edifici (es. +1 Cibo e +1 PP per Cacciatore)
                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                    if (effect instanceof EventYieldBonusEffect) {
                        // In base all'UML, questa classe modifica la resa dell'evento
                        // Nel manuale c'è un edificio esatto che fa questo:
                        extraFoodPerHunter += 1;
                        extraPPPerHunter += 1;
                    }
                }

                // 3. Calcolo dei totali
                int totalFoodToAdd = huntersCount * (foodPerHunter + extraFoodPerHunter);
                int totalPPToAdd = huntersCount * (PPperHunter + extraPPPerHunter);

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
