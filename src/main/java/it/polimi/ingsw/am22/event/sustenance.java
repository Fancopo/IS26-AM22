package it.polimi.ingsw.am22.event;
import it.polimi.ingsw.am22.BuildingEffect;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.character.CharacterType;

import java.util.ArrayList;
import java.util.List;


public class sustenance extends Event implements EventEffect {

    public Sustenance(Era era, int minPlayers) {
        super(era, minPlayers, EventType.SUSTENANCE);
    }

    @Override
    public void applyEvent(List<Player> players, char id) {
        int PPLose = 0;

        // Valori di penalità basati sull'Era corrente
        if (this.era == Era.I) {
            PPLose = -1;
        } else if (this.era == Era.II) {
            PPLose = -2;
        } else if (this.era == Era.III) {
            PPLose = -3;
        }

        for (Player player : players) {
            Tribe tribe = player.getTribe();
            if (tribe == null) continue;

            // 1. Calcola quanti personaggi ci sono (1 cibo per personaggio)
            int totalCharacters = tribe.getMembers().size();

            // 2. Calcola lo sconto base dei Raccoglitori (3 cibo per ogni raccoglitore)
            int collectorsCount = tribe.countCharacters(CharacterType.COLLECTOR);
            int totalDiscount = collectorsCount * 3;

            // 3. Calcola eventuali sconti forniti dagli Edifici
            for (Building building : tribe.getBuildings()) {
                BuildingEffect effect = building.getEffect();
                if (effect instanceof SustenanceDiscountEffect) {
                    SustenanceDiscountEffect sustenanceEffect = (SustenanceDiscountEffect) effect;
                    // L'edificio calcola da solo quanto sconto applicare in base alla tribù
                    totalDiscount += sustenanceEffect.getSustenanceDiscount(tribe);
                }
            }

            // 4. Calcola il costo finale in Cibo (non può essere minore di 0)
            int foodToPay = Math.max(0, totalCharacters - totalDiscount);

            // 5. Risoluzione del pagamento
            if (player.getFood() >= foodToPay) {
                // Ha abbastanza cibo per sfamare tutti
                player.addFood(-foodToPay); // Sottrae il cibo dal totale del giocatore
                System.out.println(player.getNickname() + " paga " + foodToPay + " cibo e sfama tutta la tribù.");

            } else {
                // Non ha abbastanza cibo: deve pagare tutto quello che ha
                int foodAvailable = player.getFood();
                int unfedCharacters = foodToPay - foodAvailable;

                // Azzera il cibo del giocatore (paga tutto quello che possiede)
                player.addFood(-foodAvailable);

                // Calcola e applica la penalità (PPLose è già negativo)
                int totalPenalty = PPLose * unfedCharacters;
                player.addPP(totalPenalty);

                System.out.println(player.getNickname() + " ha solo " + foodAvailable +
                        " cibo. Non riesce a sfamare " + unfedCharacters +
                        " personaggi e subisce " + totalPenalty + " PP di penalità.");
            }
        }
    }

}
