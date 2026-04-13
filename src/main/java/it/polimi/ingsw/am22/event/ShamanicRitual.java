package it.polimi.ingsw.am22.event;
import it.polimi.ingsw.am22.Building.Building;
import it.polimi.ingsw.am22.Building.BuildingEffect;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.character.TribeCharacter;
import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;

import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ShamanicRitual extends Event implements EventEffect {

    public ShamanicRitual(String id, Era era, int minPlayers, EventType eventType, EventEffect eventEffect) {
        super(id, era, minPlayers, EventType.SHAMANIC_RITUAL, eventEffect);
    }

    @Override
    public void applyEvent(List<Player> players, String id) {
        int PPtoAdd = 0;
        int PPtoLose = 0;

        if (this.getEra() == Era.I) {
            PPtoAdd = 5;
            PPtoLose = -3;
        } else if (this.getEra() == Era.II) {
            PPtoAdd = 10;
            PPtoLose = -5;
        } else if (this.getEra() == Era.III) {
            PPtoAdd = 15;
            PPtoLose = -7;
        }

        Map<Player, Integer> totalIconsPerPlayer = new HashMap<>();
        Map<Player, Boolean> preventLoss = new HashMap<>();
        Map<Player, Boolean> doubleWin = new HashMap<>();

        // 1. CALCOLO ICONE E MODIFICATORI EDIFICI
        for (Player player : players) {
            int baseIcons = 0;
            int extraIcons = 0;
            boolean noLoss = false;
            boolean doublePP = false;

            Tribe tribe = player.getTribe();
            if (tribe != null) {
                // Conta icone base degli Sciamani
                for (TribeCharacter character : tribe.getMembers()) {
                        baseIcons += character.getNumStars();
                }

                // Applica effetti degli Edifici per il Rituale Sciamanico
                for (Building building : tribe.getBuildings()) {
                    BuildingEffect effect = building.getEffect();
                        // Modificatore: 3 icone aggiuntive
                        extraIcons += effect.getExtraShamanIcons();
                        // Modificatore: Non perdete Punti Prestigio
                        if (effect.preventsShamanPPLoss()) noLoss = true;
                        // Modificatore: Guadagnate il doppio dei Punti Prestigio
                        if (effect.doublesShamanWinPP()) doublePP = true;
                    }
                }

            totalIconsPerPlayer.put(player, baseIcons + extraIcons);
            preventLoss.put(player, noLoss);
            doubleWin.put(player, doublePP);
        }


        if (totalIconsPerPlayer.isEmpty()) return;

        // 2. TROVA IL MASSIMO E IL MINIMO
        int maxIcons = Collections.max(totalIconsPerPlayer.values());
        int minIcons = Collections.min(totalIconsPerPlayer.values());

        // 3. ASSEGNAZIONE PREMI E PENALITÀ
        for (Player player : players) {
            int icons = totalIconsPerPlayer.get(player);

            // Vittoria: Il giocatore con più icone nella propria tribù, guadagna i Punti Prestigio indicati
            // In caso di parità, tutti i giocatori in parità ottengono i Punti Prestigio indicati
            if (icons == maxIcons) {
                int earnedPP = doubleWin.get(player) ? PPtoAdd * 2 : PPtoAdd;
                player.addPP(earnedPP);
                System.out.println(player.getNickname() + " vince il rito e ottiene " + earnedPP + " PP!");
            }

            // Sconfitta: Il giocatore con meno icone nella propria tribù, perde i Punti Prestigio indicati
            // Anche in questo caso si applica la regola della parità
            if (icons == minIcons) {
                if (preventLoss.get(player)) {
                    System.out.println(player.getNickname() + " ha meno icone, ma l'Edificio lo protegge dalla perdita di PP!");
                } else {
                    // Nota: PPtoLose è già un numero negativo, quindi usiamo addPP o un metodo equivalente
                    player.addPP(PPtoLose);
                    System.out.println(player.getNickname() + " perde il rito e subisce " + PPtoLose + " PP.");
                }
            }
        }
    }



}
