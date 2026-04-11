package it.polimi.ingsw.am22.Building;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Player;
import it.polimi.ingsw.am22.Tribe;
import it.polimi.ingsw.am22.character.CharacterType;
import it.polimi.ingsw.am22.event.EventType;
import Building.CollectionCondition;
import it.polimi.ingsw.am22.Card;
import it.polimi.ingsw.am22.Building.BuildingEffect;

public class Building extends Card {
    private int foodPrice;
    private int finalPP;
    private BuildingEffect effect;

    public Building(String id, Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect) {
        super(id, era, minPlayers);
        this.foodPrice = foodPrice;
        this.finalPP = finalPP;
        this.effect = effect;
    }

    // Sum base PP + Effect PP
    public static int FinalBuildingPP(Tribe tribe) {
        int totalSum = 0;

        for (Building b : tribe.getBuildings()) {

            // 1. Add the flat points printed on the building card
            totalSum += b.getFinalPP();

            // 2. Add the bonus points from the effect.
            totalSum += b.getEffect().calculateEndGame(tribe);

        }

        return totalSum;
    }

    // THE DECK GENERATOR (21 Cards)
  /*  public static List<Building> createAllBuildings() {
        List<Building> deck = new ArrayList<>();

        // Constructor Reminder:
        // Building(String id, Era era, int minPlayers, int foodPrice, int finalPP, BuildingEffect effect)
        // (We use 2 for minPlayers as a default since Buildings don't use this rule)

        // ERA I (6 Cards)

        // 1. Set of 6 Characters -> 5 Food
        deck.add(new Building("BLD_I_01", Era.I, 2, 4, 3,
                new CollectionRewardEffect(CollectionCondition.SET_OF_6,5)));

        // 2. Discount for Gatherers (-1 Food)
        deck.add(new Building("BLD_I_02", Era.I, 2, 4, 4,
                new SustenanceDiscountEffect(CharacterType.COLLECTOR)));

        // 3. Discount for Artists (-1 Food)
        deck.add(new Building("BLD_I_03", Era.I, 2, 5, 3,
                new SustenanceDiscountEffect(CharacterType.ARTIST)));

        // 4. Shaman: No PP Loss
        deck.add(new Building("BLD_I_04", Era.I, 2, 5, 2,
                new ShamanicModifierEffect(0, true, false)));

        // 5. Totem Bonus Space -> +1 Food
        deck.add(new Building("BLD_I_05", Era.I, 2, 3, 3,
                new TurnPhaseModifierEffect(true,false))); // Assuming this handles the Totem placement bonus


        // 6. Pair of Gatherers -> 3 Food
        deck.add(new Building("BLD_I_06", Era.I, 2, 3, 4,
                new CollectionRewardEffect(CollectionCondition.INVENTOR_PAIR, 3 )));


        // ERA II (7 Cards)

        // 7. Shaman: Double PP (x2)
        deck.add(new Building("BLD_II_01", Era.II, 2, 7, 0,
                new ShamanicModifierEffect(0, false, true)));

        // 8. 3 Shaman Stars
        deck.add(new Building("BLD_II_02", Era.II, 2, 6, 4,
                new ShamanicModifierEffect(3, false, false)));

        // 9. Discount for Inventors (-1 Food)
        deck.add(new Building("BLD_II_03", Era.II, 2, 7, 4,
                new SustenanceDiscountEffect(CharacterType.INVENTOR)));

        // 10. Hunting Event -> +1 Food & +1 PP per Hunter
        deck.add(new Building("BLD_II_04", Era.II, 2, 7, 2,
                new EventYieldBonusEffect(EventType.HUNTING, 1, 1)));

        // 11. Double Builder PP (x2)
        deck.add(new Building("BLD_II_05", Era.II, 2, 6, 4,
                new EndGameScoringEffect(0,0,null,0,true)));

        // 12. Cave Painting Event -> 1 Food per Artist
        deck.add(new Building("BLD_II_06", Era.II, 2, 5, 6,
                new EventYieldBonusEffect(EventType.CAVE_PAINTING, 1, 0)));

        // 13. End Game: Set of 6 Characters -> 6 PP
        deck.add(new Building("BLD_II_07", Era.II, 2, 5, 6,
                new EndGameScoringEffect(0,6,null,0, false)));



        // ERA III (8 Cards)

        // 14. End Game: 3 PP x Hunter
        deck.add(new Building("BLD_III_01", Era.III, 2, 8, 8,
                new EndGameScoringEffect(0,0,CharacterType.HUNTER,3,false)));

        // 15. End Game: 2 PP x Gatherer
        deck.add(new Building("BLD_III_02", Era.III, 2, 7, 6,
                new EndGameScoringEffect(0,0,CharacterType.COLLECTOR,4,false)));

        // 16. End Game: 3 PP x Shaman
        deck.add(new Building("BLD_III_03", Era.III, 2, 7, 4,
                new EndGameScoringEffect(0,0,CharacterType.SHAMAN,4,false)));

        // 17. End Game: 4 PP x Builder
        deck.add(new Building("BLD_III_04", Era.III, 2, 6, 3,
                new EndGameScoringEffect(0,0,CharacterType.BUILDER,4,false)));

        // 18. End Game: 4 PP x Artist
        deck.add(new Building("BLD_III_05", Era.III, 2, 7, 4,
                new EndGameScoringEffect(0,0,CharacterType.ARTIST,4,false)));

        // 19. End Game: 4 PP x Inventor
        deck.add(new Building("BLD_III_06", Era.III, 2, 6, 6,
                new EndGameScoringEffect(0,0,CharacterType.INVENTOR,2,false)));

        // 20. Green Arrow: Take Extra Card at Round End
        deck.add(new Building("BLD_III_07", Era.III, 2, 9, 3,
                new TurnPhaseModifierEffect(false,true)));

        // 21. The 25 PP Bomb
        deck.add(new Building("BLD_III_08", Era.III, 2, 10, 0,
                new EndGameScoringEffect(25,0,null,0,false)));

        return deck;
    }

*/
    @Override
    public void addToTribe(Player player, Tribe tribe) {
        // La carta aggiunge se stessa alla lista degli edifici della tribù
        tribe.addBuilding(this);
    }


    // Gli Edifici sopravvivono al reset di fine round
    @Override
    public boolean survivesRoundEnd() {
        return true;
    }

    // Gli Edifici nella riga inferiore vengono distrutti all'Era III
    @Override
    public boolean isDestroyedOnEraIII() {
        return true;
    }

    public void applyOnTotemPlaced(Player owner) {
        if (this.effect != null) {
            this.effect.onTotemPlaced(owner);
        }
    }

    // Controlla se questo specifico edificio fornisce l'acquisto extra.
    public boolean grantsExtraBuyAtRoundEnd() {
        return this.effect != null && this.effect.hasExtraBuyAtRoundEnd();
    }

    public int getFoodPrice() { return foodPrice; }
    public int getFinalPP() { return finalPP; }
    public BuildingEffect getEffect() { return effect; }
    public void applyOnFoodSlotPlaced(Player player) {}
}