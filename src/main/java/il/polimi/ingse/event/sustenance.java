package il.polimi.ingse.event;
import java.util.ArrayList;
import java.util.List;


public class sustenance extends Event implements EventEffect {

    public Event(char id, String type, int era, int minPlayers){
        super(id, type, era, minPlayers);
    }

    @Override
    public void applyEvent(List<Player> players, char id){

        int eraCurrent = getEra();
        int PPlose;

        if(eraCurrent == 1){ PPlose = -1;}
        if(eraCurrent == 2){ PPlose = -2;}
        if(eraCurrent == 3){ PPlose = -3;}

        for(Player p : players){
            int foodRequired = p.getTribe().getMembers().size();//per ogni giocatore vado al suo tribu e conto tutti i personaggi che si trovano nel tribu

            int discount = p.getTribe().countCharacter("Gatherer") * 3; //sconto per ogni raccoglitore
            int FoodToPay = Math.max(0, foodRequired - discount);

            // 3. Controlliamo se il giocatore ha l'Edificio che sconta il cibo
            // in base a ruoli specifici (Artisti, Inventori, Raccoglitori)
            /*for (Building building : tribe.getBuildings()) {
                if ("RoleFoodDiscount".equals(building.getType())) {
                    int numArtists = tribe.countCharacter("Artist");
                    int numInventors = tribe.countCharacter("Inventor");

                    // Sconto di 1 per ogni Artista, Inventore e Raccoglitore
                    discount += (numArtists + numInventors + numGatherers);
                    break; // Assumiamo che ci sia solo una copia di questo edificio attiva
                }
            }*/

            // 4. Applichiamo lo sconto al costo totale (il costo non può scendere sotto lo zero)
            /*equiredFood -= discount;
            if (requiredFood < 0) {
                requiredFood = 0;
            }*/

            if(FoodToPay > 0){
                int currentFood = p.getfood();

                if(currentFood >= FoodToPay){
                    int negFoodtoPay = -FoodToPay;
                    p.addFood(negFoodtoPay);
                } else{ // caso in cui non ha abbastanza cibo
                    int unpaidFood = FoodToPay - currentFood;
                    p.setFood(0);

                    int Penalty = unpaidFood * PPlose;
                    int negPenalty = -Penalty;
                    p.addPP(negPenalty);
                }
            }
        }



    }


}
