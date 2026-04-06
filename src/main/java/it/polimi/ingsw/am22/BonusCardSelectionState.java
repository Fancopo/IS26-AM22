package it.polimi.ingsw.am22;

import java.util.Collections;

/**
 * Stato temporaneo che si attiva SOLO se un giocatore possiede
 * l'edificio della pescata bonus a fine fase azioni.
 */
public class BonusCardSelectionState implements GameState {

    @Override
    public void pickBonusCard(Game game, Player player, Card bonusCard) {
        // 1. Validazione: la carta DEVE essere nella riga superiore
        if (!game.getBoard().getUpperRow().contains(bonusCard)) {
            throw new IllegalArgumentException("La carta bonus deve essere scelta dalla fila superiore!");
        }

        // 2. Assegnazione carta (Bonus gratuito, nessun check sul cibo)
        player.getTribe().addCard(bonusCard);
        //game.getBoard().removeCards(Collections.singletonList(bonusCard));

        // 3. Transizione automatica: il bonus è stato preso, procediamo con gli Eventi!
        game.setState(new EventResolutionState());
        game.resolveEvents();
    }

    @Override
    public String getPhaseName() { return "Selezione Carta Bonus"; }
}