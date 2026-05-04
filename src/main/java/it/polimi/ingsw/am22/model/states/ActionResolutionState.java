package it.polimi.ingsw.am22.model.states;

import it.polimi.ingsw.am22.model.*;
import it.polimi.ingsw.am22.model.building.Building;

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
        // 2. GESTIONE CARTE: pattern transazionale "validate-then-commit".
        //    Ogni controllo che può fallire (vincoli tessera, carte non prendibili come gli Event,
        //    cibo insufficiente per gli edifici) deve avvenire PRIMA di qualsiasi mutazione su
        //    player/tribe/board. In caso contrario, una selezione mista valida+invalida lascerebbe
        //    la carta valida già aggiunta alla tribe, e una successiva ri-selezione la duplicherebbe.
        else {
            // --- FASE 1: VALIDAZIONE (nessuna mutazione consentita qui) ---

            // 1a. Vincoli tessera (numero di carte upper/lower)
            long upperSelected = selectedCards.stream().filter(c -> game.getBoard().getUpperRow().contains(c)).count();
            long lowerSelected = selectedCards.stream().filter(c -> game.getBoard().getLowerRow().contains(c)).count();
            if (upperSelected != currentTile.getUpperCardsToTake() || lowerSelected != currentTile.getLowerCardsToTake()) {
                throw new IllegalArgumentException("Selezione carte non valida per la tessera corrente!");
            }

            // 1b. Validazione polimorfica per-carta: ogni carta dichiara da sé se è prendibile
            //     (es. Event lancia eccezione, TribeCharacter/Building accettano).
            for (Card card : selectedCards) {
                card.validatePickable();
            }

            // 1c. Calcolo costo totale e verifica cibo sufficiente, SENZA dedurlo.
            int builderDiscount = player.getTribe().getBuilderDiscount();
            int totalFoodCost = 0;
            for (Card card : selectedCards) {
                int baseCost = card.getFoodCost();
                if (baseCost > 0) {
                    totalFoodCost += Math.max(0, baseCost - builderDiscount);
                }
            }
            if (player.getFood() < totalFoodCost) {
                throw new IllegalStateException("Cibo insufficiente per acquistare le carte selezionate.");
            }

            // --- FASE 2: COMMIT (tutte le validazioni sono passate, è sicuro mutare) ---
            player.payFood(totalFoodCost);
            for (Card card : selectedCards) {
                player.getTribe().addCard(player, card);
            }
            game.getBoard().getUpperRow().removeAll(selectedCards);
            game.getBoard().getLowerRow().removeAll(selectedCards);
        }

        // 3. SPOSTAMENTO TOTEM SULL'ORDINE DI TURNO
        Slot nextSlot = game.getBoard().getTurnOrderTile().getFirstAvailableSlot();
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
            } else {
                // NESSUN BONUS: Procediamo normalmente con gli eventi
                game.setState(new EventResolutionState());
                game.resolveEvents();
            }

        } else {
            game.setActivePlayer(game.getPlayerWithLeftmostTotem());
        }
    }

    @Override
    public String getPhaseName() { return "Risoluzione Azioni"; }
}
