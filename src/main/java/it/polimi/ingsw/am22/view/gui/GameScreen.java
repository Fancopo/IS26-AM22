package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.CardDTO;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.OfferTileDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
import it.polimi.ingsw.am22.network.common.dto.TurnSlotDTO;
import it.polimi.ingsw.am22.network.common.message.ServerMessage;
import it.polimi.ingsw.am22.network.common.message.ServerMessageVisitor;
import it.polimi.ingsw.am22.network.common.message.response.EndGameMessage;
import it.polimi.ingsw.am22.network.common.message.response.ErrorMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStartedMessage;
import it.polimi.ingsw.am22.network.common.message.response.GameStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.InfoMessage;
import it.polimi.ingsw.am22.network.common.message.response.LobbyStateMessage;
import it.polimi.ingsw.am22.network.common.message.response.MatchClosedMessage;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Schermata principale di gioco.
 *
 * <p>Layout (BorderPane):
 * <ul>
 *     <li>top: header con round/era/fase e giocatore attivo;</li>
 *     <li>left: colonna giocatori con punteggio, cibo, carte possedute;</li>
 *     <li>center: board (riga superiore, riga inferiore, tessere offerta);</li>
 *     <li>right: tracciato dell'ordine di turno + pannello azioni;</li>
 *     <li>bottom: status bar per messaggi info/errore.</li>
 * </ul>
 *
 * <p>Le azioni disponibili dipendono dalla fase (estratta per substring dal
 * campo {@code currentPhase} del DTO, così siamo tolleranti ai rename nel
 * modello):
 * <ul>
 *     <li>"Totem": click su tessera offerta per piazzare il totem;</li>
 *     <li>"Action" / "Resolution": selezione carte dalle due righe + conferma;</li>
 *     <li>"Bonus": selezione carta bonus tra le carte in evidenza.</li>
 * </ul>
 * I controlli sono attivi solo quando {@code activePlayer} coincide col
 * nickname locale.
 *
 * <p>I singoli nodi carta/tessera sono placeholder testuali: sostituire con
 * immagini è solo questione di cambiare {@link #buildCardNode} e {@link #buildOfferTileNode}.
 */
public final class GameScreen implements GuiScreen {

    private final GuiApp app;
    private final BorderPane root = new BorderPane();

    // Header
    private final Label roundLabel = new Label();
    private final Label eraLabel = new Label();
    private final Label phaseLabel = new Label();
    private final Label activePlayerLabel = new Label();

    // Colonne
    private final VBox playersBox = new VBox(8);
    private final FlowPane upperRowBox = new FlowPane(8, 8);
    private final FlowPane lowerRowBox = new FlowPane(8, 8);
    private final FlowPane offerTilesBox = new FlowPane(8, 8);
    private final VBox turnOrderBox = new VBox(6);

    // Action panel
    private final VBox actionBox = new VBox(10);
    private final Label actionHint = new Label();
    private final Button confirmPickButton = new Button("Confirm card selection");
    private final Label statusLabel = new Label();

    /** Id delle carte selezionate per la pickCards. */
    private final Set<String> pickedCardIds = new HashSet<>();

    public GameScreen(GuiApp app) {
        this.app = app;
        buildUi();
        GameStateDTO cached = app.getSession().getLatestGameState();
        if (cached != null) render(cached);
    }

    @Override
    public Parent getRoot() {
        return root;
    }

    @Override
    public void onServerMessage(ServerMessage message) {
        message.accept(new ServerMessageVisitor() {
            @Override public void visit(GameStateMessage m) { render(m.gameState()); }
            @Override public void visit(GameStartedMessage m) { render(m.initialGameState()); }
            @Override public void visit(ErrorMessage m) { statusLabel.setText("Error: " + m.message()); }
            @Override public void visit(InfoMessage m) { statusLabel.setText(m.message()); }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
        });
    }

    // -------------------- costruzione layout --------------------

    private void buildUi() {
        HBox header = new HBox(20, roundLabel, eraLabel, phaseLabel, new Label("|"), activePlayerLabel);
        header.setPadding(new Insets(10));
        header.setAlignment(Pos.CENTER_LEFT);
        root.setTop(header);

        ScrollPane playersScroll = new ScrollPane(playersBox);
        playersScroll.setFitToWidth(true);
        playersScroll.setPrefWidth(260);
        playersBox.setPadding(new Insets(10));
        root.setLeft(playersScroll);

        VBox center = new VBox(12,
                new Label("Upper row"),
                upperRowBox,
                new Label("Lower row"),
                lowerRowBox,
                new Label("Offer tiles"),
                offerTilesBox);
        center.setPadding(new Insets(10));
        root.setCenter(center);

        actionBox.getChildren().addAll(new Label("Actions"), actionHint, confirmPickButton);
        confirmPickButton.setOnAction(e -> submitPick());
        VBox right = new VBox(16,
                new Label("Turn order"),
                turnOrderBox,
                actionBox);
        right.setPadding(new Insets(10));
        right.setPrefWidth(260);
        root.setRight(right);

        root.setBottom(statusLabel);
        statusLabel.setPadding(new Insets(6, 10, 6, 10));
    }

    // -------------------- rendering di uno stato --------------------

    private void render(GameStateDTO state) {
        roundLabel.setText("Round: " + state.currentRound());
        eraLabel.setText("Era: " + state.currentEra());
        phaseLabel.setText("Phase: " + state.currentPhase());
        activePlayerLabel.setText("Active: " + (state.activePlayer() == null ? "-" : state.activePlayer()));

        renderPlayers(state.players());
        renderCardRow(upperRowBox, state.upperRow());
        renderCardRow(lowerRowBox, state.lowerRow());
        renderOfferTiles(state.offerTrack(), state);
        renderTurnOrder(state.turnOrder());
        renderActionPanel(state);

        // Dopo un aggiornamento di stato le selezioni precedenti non hanno più senso.
        pickedCardIds.clear();
        refreshCardSelections();
    }

    private void renderPlayers(List<PlayerDTO> players) {
        playersBox.getChildren().clear();
        for (PlayerDTO p : players) {
            VBox card = new VBox(2);
            card.setPadding(new Insets(6));
            // GRAPHIC PLACEHOLDER: questo VBox corrisponde al "pannello giocatore".
            // Per lo sfondo: assegnare style/CSS o sostituire con ImageView.
            String header = (p.active() ? "> " : "  ") + p.nickname()
                    + " [" + p.totemColor() + "]";
            card.getChildren().addAll(
                    new Label(header),
                    new Label("PP: " + p.prestigePoints() + " (proj " + p.projectedFinalPrestigePoints() + ")"),
                    new Label("Food: " + p.food()),
                    new Label("Tribe: " + countByType(p.tribeCharacters())),
                    new Label("Buildings: " + countByType(p.buildings()))
            );
            playersBox.getChildren().add(card);
        }
    }

    private void renderCardRow(FlowPane container, List<CardDTO> cards) {
        container.getChildren().clear();
        for (CardDTO c : cards) {
            container.getChildren().add(buildCardNode(c));
        }
    }

    /**
     * Costruisce un nodo "carta": bottone toggle usato in ActionResolutionState per
     * selezionare le carte da pescare, o bottone normale nelle altre fasi.
     * <p>GRAPHIC PLACEHOLDER: il nodo contiene un semplice Label con l'id;
     * sostituire con un ImageView che mostra l'immagine della carta.
     */
    private ToggleButton buildCardNode(CardDTO c) {
        ToggleButton tb = new ToggleButton(c.id() + "\n" + c.detailType());
        tb.setPrefSize(110, 140);
        tb.setUserData(c.id());
        tb.setSelected(pickedCardIds.contains(c.id()));
        tb.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) pickedCardIds.add(c.id());
            else pickedCardIds.remove(c.id());
        });
        return tb;
    }

    private void renderOfferTiles(List<OfferTileDTO> tiles, GameStateDTO state) {
        offerTilesBox.getChildren().clear();
        boolean myTurn = isMyTurn(state);
        boolean totemPhase = isTotemPhase(state.currentPhase());
        for (OfferTileDTO t : tiles) {
            offerTilesBox.getChildren().add(buildOfferTileNode(t, myTurn && totemPhase));
        }
    }

    /**
     * Costruisce un bottone per la tessera offerta; l'azione invia
     * {@code placeTotem(letter)} al server.
     * <p>GRAPHIC PLACEHOLDER: sostituire il label con l'immagine della tessera.
     */
    private Button buildOfferTileNode(OfferTileDTO t, boolean clickable) {
        String text = t.letter()
                + "\nup=" + t.upperCardsToTake()
                + " low=" + t.lowerCardsToTake()
                + " food=" + t.foodReward()
                + (t.occupiedBy() == null ? "" : "\nby " + t.occupiedBy());
        Button b = new Button(text);
        b.setPrefSize(110, 90);
        b.setDisable(!clickable || t.occupiedBy() != null);
        b.setOnAction(e -> {
            try {
                app.getSession().getClientController().placeTotem(t.letter());
            } catch (RuntimeException ex) {
                statusLabel.setText("Place failed: " + ex.getMessage());
            }
        });
        return b;
    }

    private void renderTurnOrder(List<TurnSlotDTO> turnOrder) {
        turnOrderBox.getChildren().clear();
        for (TurnSlotDTO slot : turnOrder) {
            String t = "pos " + slot.positionIndex()
                    + " food+" + slot.foodBonus()
                    + (slot.lastSpace() ? " (last)" : "")
                    + (slot.occupiedBy() == null ? "" : " -> " + slot.occupiedBy());
            turnOrderBox.getChildren().add(new Label(t));
        }
    }

    /**
     * Aggiorna il pannello azioni in base alla fase corrente. Mostra un hint
     * all'utente e abilita/disabilita i bottoni pertinenti.
     */
    private void renderActionPanel(GameStateDTO state) {
        boolean myTurn = isMyTurn(state);
        String phase = state.currentPhase() == null ? "" : state.currentPhase();
        if (!myTurn) {
            actionHint.setText("Waiting for " + state.activePlayer() + "...");
            confirmPickButton.setDisable(true);
            return;
        }
        if (isTotemPhase(phase)) {
            actionHint.setText("Your turn: click an offer tile to place your totem.");
            confirmPickButton.setDisable(true);
        } else if (isActionPhase(phase)) {
            actionHint.setText("Your turn: select cards, then confirm.");
            confirmPickButton.setDisable(false);
        } else if (isBonusPhase(phase)) {
            actionHint.setText("Your turn: pick a bonus card (select one and confirm).");
            confirmPickButton.setDisable(false);
        } else {
            actionHint.setText("Your turn (" + phase + ").");
            confirmPickButton.setDisable(true);
        }
    }

    private void submitPick() {
        GameStateDTO state = app.getSession().getLatestGameState();
        if (state == null) return;
        String phase = state.currentPhase() == null ? "" : state.currentPhase();
        List<String> ids = new ArrayList<>(pickedCardIds);
        try {
            if (isBonusPhase(phase)) {
                if (ids.size() != 1) {
                    statusLabel.setText("Select exactly one bonus card.");
                    return;
                }
                app.getSession().getClientController().pickBonusCard(ids.get(0));
            } else {
                app.getSession().getClientController().pickCards(ids);
            }
            pickedCardIds.clear();
            refreshCardSelections();
        } catch (RuntimeException ex) {
            statusLabel.setText("Submit failed: " + ex.getMessage());
        }
    }

    /** Reset visivo dei ToggleButton quando puliamo la selezione. */
    private void refreshCardSelections() {
        deselect(upperRowBox);
        deselect(lowerRowBox);
    }

    private void deselect(FlowPane container) {
        for (var n : container.getChildren()) {
            if (n instanceof ToggleButton tb) tb.setSelected(false);
        }
    }

    // -------------------- helper --------------------

    private boolean isMyTurn(GameStateDTO state) {
        String me = app.getSession().getLocalNickname();
        return me != null && me.equalsIgnoreCase(state.activePlayer());
    }

    private boolean isTotemPhase(String phase) {
        return phase != null && phase.toLowerCase().contains("totem");
    }

    private boolean isActionPhase(String phase) {
        if (phase == null) return false;
        String p = phase.toLowerCase();
        return p.contains("action") || p.contains("resolution") && !p.contains("event");
    }

    private boolean isBonusPhase(String phase) {
        return phase != null && phase.toLowerCase().contains("bonus");
    }

    private String countByType(List<CardDTO> cards) {
        if (cards == null || cards.isEmpty()) return "-";
        return cards.size() + " card(s)";
    }
}
