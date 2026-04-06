package it.polimi.ingsw.am22;

import it.polimi.ingsw.am22.Building.Building;

import java.util.List;

public class ActionResolutionState implements GameState {
    // Unico metodo che il Controller chiamerà.
    // Se il giocatore è sulla tessera A (solo cibo), selectedCards sarà una lista vuota.
    @Override
    public void pickCards(Game game, Player player, List<Card> selectedCards) {
        OfferTile currentTile = game.getBoard().getOfferTrack().stream()
                .filter(t -> t.getOccupiedBy() == player.getTotem())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Il giocatore non è sul tracciato offerte!"));

        // 1. GESTIONE CIBO (es. Tessera A)
        if (currentTile.getFoodReward() > 0 && selectedCards.isEmpty()) {
            player.addFood(currentTile.getFoodReward());
        }
        // 2. GESTIONE CARTE E VALIDAZIONE
        else {
            long upperSelected = selectedCards.stream().filter(c -> game.getBoard().getUpperRow().contains(c)).count();
            long lowerSelected = selectedCards.stream().filter(c -> game.getBoard().getLowerRow().contains(c)).count();

            // Validazione vincoli tessera
            if (upperSelected != currentTile.getUpperCardsToTake() || lowerSelected != currentTile.getLowerCardsToTake()) {
                throw new IllegalArgumentException("Selezione carte non valida per la tessera corrente!");
            }

            // Pagamento Edifici con sconto
            int totalFoodCost = 0;
            int builderDiscount = player.getTribe().getBuilderDiscount();
            //prezzo scontato
            for (Card card : selectedCards) {
                int baseCost = card.getFoodCost();
                if (baseCost > 0) {
                    totalFoodCost += Math.max(0, baseCost - builderDiscount);
                }
            }
            player.payFood(totalFoodCost); // Lancia eccezione se il cibo non basta

            // Aggiunta carte e pulizia plancia
            for (Card card : selectedCards) {
                player.getTribe().addCard(card);
            }
            game.getBoard().getUpperRow().removeAll(selectedCards);
            game.getBoard().getLowerRow().removeAll(selectedCards);
        }

        // 3. SPOSTAMENTO TOTEM SULL'ORDINE DI TURNO
        Slot nextSlot = game.getBoard().getTurnOrderTile().getFirstAvailableSlot();
        currentTile.clear();
        nextSlot.placeTotem(player.getTotem());
        player.getTotem().moveToTurnOrder(nextSlot);

        // Bonus/Malus Ordine di Turno
        if (nextSlot.getFoodBonus() > 0) {
            // 1. Il giocatore prende il cibo base dello slot
            player.addFood(nextSlot.getFoodBonus());

            // 2. IL TRIGGER: Svegliamo tutti gli edifici del giocatore!
            // Questo è il momento esatto che hai descritto.
            if (player.getTribe() != null) {
                for (Building b : player.getTribe().getBuildings()) {
                    b.applyOnFoodSlotPlaced(player);
                }
            }
        }
        if (nextSlot.isLastSpace()) {
            if (player.getFood() >= 1) {
                player.addFood(-1);
            } else {
                player.addPP(-2);
            }
        }

        // ==========================================
        // CONTROLLO FINE FASE E PESCATA BONUS EXTRA
        // ==========================================
        if (game.getBoard().getTurnOrderTile().getOccupiedSlotsCount() == game.getPlayers().size()) {

            // Tutti i totem sono tornati. Controlliamo il flag `extraBuyAtRoundEnd`.
            Player bonusPlayer = null;
            for (Player p : game.getPlayers()) {
                if (p.hasExtraBuyAtRoundEnd()) {
                    bonusPlayer = p;
                    break;
                }
            }

            if (bonusPlayer != null) {
                // IL GIOCATORE HA L'EDIFICIO: Mettiamo in pausa e andiamo nello stato bonus
                game.setActivePlayer(bonusPlayer);
                game.setState(new BonusCardSelectionState());
                //game.notifyObservers();
            } else {
                // NESSUN BONUS: Procediamo normalmente con gli eventi
                game.setState(new EventResolutionState());
                game.resolveEvents();
            }

        } else {
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
           // game.notifyObservers();
        }
    }

    @Override
    public String getPhaseName() { return "Risoluzione Azioni"; }
}