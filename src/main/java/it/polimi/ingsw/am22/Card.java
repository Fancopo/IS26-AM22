package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Era;
import it.polimi.ingsw.am22.Game;
import it.polimi.ingsw.am22.Tribe;

public abstract class Card {
    private String id;
    private Era era;
    private int minPlayers;

    public Card(String id, Era era, int minPlayers) {
        this.id = id;
        this.era = era;
        this.minPlayers = minPlayers;
    }

    public abstract void addToTribe(Tribe tribe);

    public int getFoodCost() {return 0;} // Di default le carte non costano cibo

    public void onRoundEndTrigger(Game game) {} // Nessun comportamento di default

    public int getTriggerPriority() {return 0;} // Determina l'ordine di risoluzione (0 = normale, 1 = ritardato).
    // Serve per far scattare il Sostentamento per ultimo.

    public boolean survivesRoundEnd() {return false;}  // Definisce se la carta rimane sulla plancia a fine round.
    // Di base, le carte (Personaggi, Eventi) vengono scartate.

    public boolean isDestroyedOnEraIII() {return false;} // Definisce se la carta viene distrutta al cambio verso l'Era III.


    public String getId() {return id;}
    public Era getEra() {return era;}
    public int getMinPlayers() {return minPlayers;}
}