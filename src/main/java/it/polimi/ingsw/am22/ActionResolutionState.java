package it.polimi.ingsw.am22;

import java.util.List;

public class ActionResolutionState implements GameState {
    // Unico metodo che il Controller chiamerà.
    // Se il giocatore è sulla tessera A (solo cibo), selectedCards sarà una lista vuota.
    @Override
    public void pickCards(Game game, Player player, List<Card> selectedCards) {
        OfferTile currentTile = game.getBoard().getOfferTrack().stream()
                .filter(t -> t.getOccupyingTotem() == player.getTotem())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Il giocatore non è sul tracciato offerte!"));

        // 1. GESTIONE CIBO (es. Tessera A)
        if (currentTile.getFoodReward() > 0 && selectedCards.isEmpty()) {
            player.addFood(currentTile.getFoodReward());
        }
        // 2. GESTIONE CARTE E VALIDAZIONE
        else {
            int upperSelected = selectedCards.stream().filter(c -> game.getBoard().getUpperRow().contains(c)).count();
            int lowerSelected = selectedCards.stream().filter(c -> game.getBoard().getLowerRow().contains(c)).count();

            // Validazione vincoli tessera
            if (upperSelected > currentTile.getUpperCardsToTake() || lowerSelected > currentTile.getLowerCardsToTake()) {
                throw new IllegalArgumentException("Selezione carte non valida per la tessera corrente!");
            }

            // Pagamento Edifici con sconto
            int totalFoodCost = 0;
            int builderDiscount = player.getTribe().getBuilderDiscount();

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
        player.getTotem().moveToSlot(nextSlot);

        // Bonus/Malus Ordine di Turno
        if (nextSlot.getFoodBonus() > 0) {
            player.addFood(nextSlot.getFoodBonus());
        }
        if (nextSlot.isLastSpace()) {
            if (player.getFood() >= 1) {
                player.addFood(-1);
            } else {
                player.addPP(-2);
            }
        }

        // 4. TRANSIZIONE: Prossimo giocatore o Fase Eventi?
        if (game.getBoard().getTurnOrderTile().getOccupiedSlotsCount() == game.getPlayers().size()) {
            game.setState(new EventResolutionState());
            game.resolveEvents();
        } else {
            // Rimaniamo nello stesso stato, ma cambiamo il giocatore attivo
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
            game.notifyObservers(); // Notifica la View che tocca al prossimo
        }
    }

    @Override
    public String getPhaseName() { return "Risoluzione Azioni"; }
}