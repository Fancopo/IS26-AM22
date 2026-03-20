package il.polimi.ingse.event;
import il.polimi.ingse.character.Shaman;
import il.polimi.ingse.character.TribeCharacter;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ShamanicRitual extends Event implements EventEffect {


    @Override
    public void applyEvent(List<Player> players, char id)

    int CountStars;
    int currentEra = getEra();
    int PPtoAdd;
    int PPtoLose;

    if(currentEra == 1){
        PPtoAdd = 5;
        PPtoLose = -3;
    }
    if(currentEra == 2){
        PPtoAdd = 10;
        PPtoLose = -5;
    }

    if(currentEra == 3){
        PPtoAdd = 15;
        PPtoLose = -7;
    }

    Map<Player, Integer> playIconsCount = new HashMap<>();

    //se considero anche effetto di building
    //Map<Player, Boolean> hasDoublePPBuilding = new HashMap<>();
    //Map<Player, Boolean> hasNoLossBuilding = new HashMap<>();
    int maxIcons = -1;
    int minIcons = Integer.MAX_VALUE;

//conto le icone degli Sciamani per ogni giocatore
    for(Player player : players){
        int totalIcons = 0;
        //boolean doublePP = false;
        //boolean noLoss = false;

        for(TribeCharacter character : player.getTribe().getMembers()){
            //verifichiamo se il personaggio e uno Sciamano
            if(character == "Shaman"){
                totalIcons += ((Shaman)character).getNumStars();
            }
        }

       /* for (Building building : player.getTribe().getBuildings()) {
            String bType = building.getType(); // Ereditato da Card

            if (bType.equals("ExtraShamanIcons")) {
                totalIcons += 3; // L'edificio fornisce 3 icone aggiuntive
            } else if (bType.equals("DoubleShamanPP")) {
                doublePP = true; // Raddoppia i PP vinti [cite: 265]
            } else if (bType.equals("NoShamanLoss")) {
                noLoss = true;   // Previene la perdita di PP [cite: 247, 250]
            }

        */
    }

           // C'è un edificio di Era II/III che fornisce 3 icone aggiuntive durante questo evento.
            //Qui andrebbe integrato un controllo su player.getTribe().getBuildings() per applicare quel bonus.
            //hasDoublePPBuilding.put(player, doublePP);
            //hasNoLossBuilding.put(player, noLoss);

    playerIconsCount.put(player, totalIcons);

    if (totalIcons > maxIcons) maxIcons = totalIcons;
    if (totalIcons < minIcons) minIcons = totalIcons;

    for(Player player : players){
        int icons = playerIconsCount.get(player);

        if(icons == maxIcons){

            player.addPP(ppToWin);
            //NOTA EDIFICI: C'è un edificio che raddoppia i PP guadagnati se si ha la maggioranza.
            //int pointsEarned = ppToWin;
            //
            //                // Applichiamo l'edificio dei punti doppi se posseduto
            //                if (hasDoublePPBuilding.get(player)) {
            //                    pointsEarned *= 2;
            //                }
            //                player.addPP(pointsEarned);
        }
        if (icons == minIcons){
            // NOTA EDIFICI: Un altro edificio impedisce di perdere PP in questo evento.
            player.addPP(-ppToLose);
        }
    }



}
