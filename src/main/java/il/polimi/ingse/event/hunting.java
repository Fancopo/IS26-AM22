package il.polimi.ingse.event;
import java.util.ArrayList;
import java.util.List;
public class hunting extends Event implements EventEffect{

    public Event(char id, String type, int era, int minPlayers){
        super(id, type, era, minPlayers);
    }


    @Override
    public void applyEvent(List<Player> players, char id){

        int eraCurrent = getEra();
        int PPperHunter;

        if(eraCurrent == 1){PPperHunter = 1;}
        if(eraCurrent == 2){PPperHunter = 2;}
        if(eraCurrent == 3){PPperHunter = 3;}


        for(Player p : players){
            int numhunter = p.getTribe().countCharacter("Hunter");

            if (numHunters == 0) {
                continue;
            }
            if(numhunter > 0){
                int foodtoAdd = numhunter;
                p.addFood(foodtoAdd);

                int PPtoAdd = numhunter * PPperHunter;
                p.addPP(PPtoAdd);
                // 3. Controlliamo se il giocatore possiede l'Edificio bonus per la Caccia
                /*boolean hasHuntingBonusBuilding = false;
                for (Building building : tribe.getBuildings()) {
                    if ("HuntingBonus".equals(building.getType())) {
                        hasHuntingBonusBuilding = true;
                        break;
                    }
                }

                // 4. Applichiamo i bonus dell'edificio, se presente
                if (hasHuntingBonusBuilding) {
                    foodToGain += (numHunters * 1); // +1 Cibo per cacciatore
                    ppToGain += (numHunters * 1);   // +1 PP per cacciatore
                }*/
            }
        }
    }

}
