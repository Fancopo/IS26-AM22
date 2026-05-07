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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Schermata principale di gioco.
 *
 * <p>Layout (BorderPane) ispirato allo screenshot BGA del gioco Mesos:
 * <ul>
 *     <li>top: header con titolo della fase corrente, round, era;</li>
 *     <li>center: board centrale → riga superiore, tessere offerta + tracciato
 *         turni, riga inferiore;</li>
 *     <li>right: pannelli giocatori (avversari) impilati verticalmente, con
 *         totem, PP, e icone risorsa;</li>
 *     <li>bottom: barra del giocatore locale (lo schienale giallo nello screenshot)
 *         con avatar e tutte le proprie risorse;</li>
 *     <li>(status bar nascosta in fondo alla barra locale).</li>
 * </ul>
 *
 * <p>Tutti i nodi grafici (carte, tessere, totem, icone) sono caricati via
 * {@link ImageCache}: se il PNG corrispondente esiste nei resources, viene
 * mostrato; altrimenti si vede un placeholder colorato con etichetta. Per
 * sostituire i placeholder è sufficiente droppare i file in
 * {@code src/main/resources/images/...} con il naming convention attesa
 * (vedi {@link ImageCache}).
 *
 * <p>Le azioni disponibili dipendono dalla fase, individuata per substring
 * sul campo {@code currentPhase} del DTO:
 * <ul>
 *     <li>"Totem" → click su una tessera offerta libera per piazzare il totem;</li>
 *     <li>"Action" / "Resolution" → selezione carte dalle due righe + Conferma;</li>
 *     <li>"Bonus" → selezione di una sola carta tra quelle proposte + Conferma.</li>
 * </ul>
 * I controlli sono attivi solo se {@code activePlayer} coincide col nickname locale.
 */
public final class GameScreen implements GuiScreen {

    // Dimensioni di display (raddoppiate nei PNG → vedi linee guida asset).
    private static final double CARD_W = 110;
    private static final double CARD_H = 150;
    private static final double TILE_W = 110;
    private static final double TILE_H = 90;
    private static final double TOTEM_S = 32;
    private static final double ICON_S = 22;

    private final GuiApp app;
    private final BorderPane root = new BorderPane();

    // Header
    private final Label headerTitle = new Label();
    private final Label roundLabel = new Label();
    private final Label eraLabel = new Label();
    private final Label phaseLabel = new Label();

    // Board centrale
    private final FlowPane upperRowBox = new FlowPane(8, 8);
    private final FlowPane lowerRowBox = new FlowPane(8, 8);
    private final FlowPane offerTilesBox = new FlowPane(8, 8);
    private final HBox turnTrackBox = new HBox(4);

    // Right column: lista pannelli avversari
    private final VBox playersBox = new VBox(10);

    // Bottom: pannello giocatore locale
    private final BorderPane localBar = new BorderPane();

    // Action panel (dentro la colonna destra, in basso)
    private final VBox actionBox = new VBox(8);
    private final Label actionHint = new Label();
    private final Button confirmPickButton = new Button("Conferma");
    private final Label statusLabel = new Label();

    /** Id delle carte attualmente selezionate per la pickCards. */
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
            @Override public void visit(ErrorMessage m) {
                statusLabel.setText("Errore: " + m.message());
                clearCardSelection();
            }
            @Override public void visit(InfoMessage m) { statusLabel.setText(m.message()); }
            @Override public void visit(LobbyStateMessage m) {}
            @Override public void visit(EndGameMessage m) {}
            @Override public void visit(MatchClosedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchJoinedMessage m) {}
            @Override public void visit(it.polimi.ingsw.am22.network.common.message.response.MatchesListMessage m) {}
        });
    }

    // =====================================================================
    // costruzione layout (statica: i nodi vengono solo riempiti in render)
    // =====================================================================

    private void buildUi() {
        root.getStyleClass().add("game-root");

        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setRight(buildRightColumn());
        root.setBottom(buildBottom());
    }

    private Node buildHeader() {
        headerTitle.getStyleClass().add("game-header-title");
        headerTitle.setText("In attesa...");

        Node spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20,
                headerTitle, spacer,
                phaseLabel, new Label("•"), eraLabel, new Label("•"), roundLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("game-header");
        return header;
    }

    private Node buildCenter() {
        upperRowBox.setAlignment(Pos.CENTER);
        lowerRowBox.setAlignment(Pos.CENTER);
        offerTilesBox.setAlignment(Pos.CENTER);
        upperRowBox.getStyleClass().add("board-row");
        lowerRowBox.getStyleClass().add("board-row");
        offerTilesBox.getStyleClass().add("board-row");

        turnTrackBox.setAlignment(Pos.CENTER);
        turnTrackBox.getStyleClass().add("turn-track");

        Label upLbl = new Label("Riga superiore");
        Label lowLbl = new Label("Riga inferiore");
        Label tileLbl = new Label("Tessere offerta");
        Label trackLbl = new Label("Tracciato turni");
        for (Label l : new Label[]{upLbl, lowLbl, tileLbl, trackLbl}) {
            l.getStyleClass().add("board-section-label");
        }

        VBox center = new VBox(10,
                upLbl, upperRowBox,
                tileLbl, offerTilesBox,
                trackLbl, turnTrackBox,
                lowLbl, lowerRowBox);
        center.setPadding(new Insets(14));
        center.setAlignment(Pos.TOP_CENTER);

        ScrollPane scroll = new ScrollPane(center);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        return scroll;
    }

    private Node buildRightColumn() {
        playersBox.setPadding(new Insets(10));
        playersBox.setFillWidth(true);

        actionBox.getStyleClass().add("action-panel");
        actionHint.getStyleClass().add("action-hint");
        actionHint.setWrapText(true);
        actionHint.setMaxWidth(240);
        confirmPickButton.getStyleClass().add("confirm-button");
        confirmPickButton.setOnAction(e -> submitPick());
        Label actionsLbl = new Label("Azioni");
        actionsLbl.getStyleClass().add("board-section-label");
        actionBox.getChildren().addAll(actionsLbl, actionHint, confirmPickButton);

        VBox right = new VBox(12, playersBox, actionBox);
        right.setPadding(new Insets(10));
        right.setPrefWidth(300);

        ScrollPane scroll = new ScrollPane(right);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefWidth(310);
        return scroll;
    }

    private Node buildBottom() {
        localBar.getStyleClass().add("local-bar");
        statusLabel.getStyleClass().add("status-bar");
        VBox v = new VBox(localBar, statusLabel);
        return v;
    }

    // =====================================================================
    // rendering di uno stato
    // =====================================================================

    private void render(GameStateDTO state) {
        roundLabel.setText("Round " + state.currentRound());
        eraLabel.setText("Era " + state.currentEra());
        phaseLabel.setText("Fase: " + state.currentPhase());
        headerTitle.setText(headerTitleFor(state));

        // Le selezioni precedenti non hanno più senso con un nuovo stato:
        // svuotiamo PRIMA di rebuildare le card così le ToggleButton
        // partono deselezionate e lo stato visivo resta sincronizzato
        // con pickedCardIds.
        pickedCardIds.clear();

        renderUpperRow(state);
        renderLowerRow(state);
        renderOfferTiles(state);
        renderTurnTrack(state.turnOrder());
        renderOpponents(state);
        renderLocalBar(state);
        renderActionPanel(state);
    }

    /** Titolo dinamico in base alla fase, simile alla barra rossa BGA. */
    private String headerTitleFor(GameStateDTO state) {
        if (!isMyTurn(state)) {
            return "Turno di " + (state.activePlayer() == null ? "—" : state.activePlayer());
        }
        String phase = state.currentPhase() == null ? "" : state.currentPhase();
        if (isTotemPhase(phase))   return "Scegli una Tessera Offerta su cui posizionare il tuo totem";
        if (isActionPhase(phase))  return "Seleziona le carte da pescare e conferma";
        if (isBonusPhase(phase))   return "Scegli una carta bonus";
        return "Tocca a te";
    }

    // ------ board rows ------

    private void renderUpperRow(GameStateDTO state) {
        upperRowBox.getChildren().clear();
        boolean canPick = isMyTurn(state) && (isActionPhase(state.currentPhase()) || isBonusPhase(state.currentPhase()));
        for (CardDTO c : state.upperRow()) {
            upperRowBox.getChildren().add(buildCardNode(c, canPick, "UP"));
        }
    }

    private void renderLowerRow(GameStateDTO state) {
        lowerRowBox.getChildren().clear();
        boolean canPick = isMyTurn(state) && (isActionPhase(state.currentPhase()) || isBonusPhase(state.currentPhase()));
        for (CardDTO c : state.lowerRow()) {
            lowerRowBox.getChildren().add(buildCardNode(c, canPick, "LOW"));
        }
    }

    /**
     * Costruisce un nodo "carta": ToggleButton con un'immagine (o placeholder).
     * Il path atteso è {@code /images/cards/{id}.png} → quando il PNG verrà
     * fornito, sostituirà automaticamente il placeholder.
     */
    private ToggleButton buildCardNode(CardDTO c, boolean clickable, String fallbackLabel) {
        Color color = colorForCardCategory(c.category());
        String label = (c.id() == null ? fallbackLabel : c.id())
                + (c.detailType() == null ? "" : "\n" + c.detailType());
        Node graphic = ImageCache.node(ImageCache.cardPath(c.id()), CARD_W, CARD_H, label, color);

        ToggleButton tb = new ToggleButton();
        tb.setGraphic(graphic);
        tb.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        tb.getStyleClass().add("card-toggle");
        tb.setUserData(c.id());
        tb.setSelected(pickedCardIds.contains(c.id()));
        tb.setDisable(!clickable);
        tb.selectedProperty().addListener((obs, was, isNow) -> {
            if (isNow) pickedCardIds.add(c.id());
            else pickedCardIds.remove(c.id());
        });
        return tb;
    }

    private Color colorForCardCategory(String category) {
        if (category == null) return Color.web("#88aabb");
        return switch (category.toUpperCase()) {
            case "CHARACTER" -> Color.web("#d8a96b");
            case "BUILDING"  -> Color.web("#9b6d3f");
            case "EVENT"     -> Color.web("#5a3b8a");
            default          -> Color.web("#88aabb");
        };
    }

    // ------ offer tiles ------

    private void renderOfferTiles(GameStateDTO state) {
        offerTilesBox.getChildren().clear();
        boolean myTurn = isMyTurn(state);
        boolean totemPhase = isTotemPhase(state.currentPhase());
        for (OfferTileDTO t : state.offerTrack()) {
            offerTilesBox.getChildren().add(buildOfferTileNode(t, state, myTurn && totemPhase));
        }
    }

    /**
     * Costruisce una tessera offerta cliccabile. Il PNG atteso è
     * {@code /images/tiles/tile_X.png}. Se la tessera è occupata, sopra
     * l'immagine appare il totem del giocatore corrispondente.
     */
    private Node buildOfferTileNode(OfferTileDTO t, GameStateDTO state, boolean clickable) {
        String fallback = String.valueOf(t.letter())
                + "\nU+" + t.upperCardsToTake()
                + " L+" + t.lowerCardsToTake()
                + "\n+" + t.foodReward() + " food";
        Node tileImg = ImageCache.node(ImageCache.tilePath(t.letter()), TILE_W, TILE_H,
                fallback, Color.web("#c69862"));

        StackPane content = new StackPane(tileImg);
        if (t.occupiedBy() != null) {
            String color = totemColorForNickname(state, t.occupiedBy());
            Node totem = ImageCache.icon(ImageCache.totemPath(color), TOTEM_S,
                    initialOf(t.occupiedBy()), ImageCache.colorFromName(color));
            StackPane.setAlignment(totem, Pos.CENTER);
            content.getChildren().add(totem);
        }

        Button b = new Button();
        b.setGraphic(content);
        b.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        b.getStyleClass().add("offer-tile");
        if (t.occupiedBy() != null) b.getStyleClass().add("offer-tile-occupied");
        b.setDisable(!clickable || t.occupiedBy() != null);
        b.setOnAction(e -> {
            try {
                app.getSession().getClientController().placeTotem(t.letter());
            } catch (RuntimeException ex) {
                statusLabel.setText("Piazzamento fallito: " + ex.getMessage());
            }
        });
        return b;
    }

    // ------ turn track ------

    private void renderTurnTrack(List<TurnSlotDTO> slots) {
        turnTrackBox.getChildren().clear();
        for (TurnSlotDTO slot : slots) {
            VBox cell = new VBox(2);
            cell.setAlignment(Pos.CENTER);
            cell.getStyleClass().add("turn-slot");
            if (slot.lastSpace()) cell.getStyleClass().add("turn-slot-last");

            Label idx = new Label("#" + slot.positionIndex());
            Label food = new Label("+" + slot.foodBonus() + " 🍖");
            cell.getChildren().addAll(idx, food);
            if (slot.occupiedBy() != null) {
                cell.getChildren().add(new Label(slot.occupiedBy()));
            }
            turnTrackBox.getChildren().add(cell);
        }
    }

    // ------ players (right column) ------

    private void renderOpponents(GameStateDTO state) {
        playersBox.getChildren().clear();
        String me = app.getSession().getLocalNickname();
        for (PlayerDTO p : state.players()) {
            if (me != null && me.equalsIgnoreCase(p.nickname())) continue;
            playersBox.getChildren().add(buildPlayerPanel(p, false));
        }
    }

    private Node buildPlayerPanel(PlayerDTO p, boolean local) {
        VBox box = new VBox(6);
        box.getStyleClass().add(local ? "local-bar" : "player-panel");
        if (p.active()) box.getStyleClass().add("player-panel-active");

        Node totem = ImageCache.icon(ImageCache.totemPath(p.totemColor()), TOTEM_S,
                initialOf(p.nickname()), ImageCache.colorFromName(p.totemColor()));

        Label name = new Label(p.nickname());
        name.getStyleClass().add(local ? "local-bar-name" : "player-name");
        Label pp = new Label(p.prestigePoints() + " ★");
        pp.getStyleClass().add("player-pp");

        HBox header = new HBox(8, totem, name, spacer(), pp);
        header.setAlignment(Pos.CENTER_LEFT);

        FlowPane resources = new FlowPane(6, 4);
        resources.getChildren().addAll(
                resourceCell("food",   p.food(),                          "Cibo"),
                resourceCell("stone",  countByDetail(p.tribeCharacters(), "STONE"), "Pietra"),
                resourceCell("arrow",  countByDetail(p.tribeCharacters(), "ARROW"), "Frecce"),
                resourceCell("hand",   countByDetail(p.tribeCharacters(), "HAND"),  "Mano"),
                resourceCell("eye",    countByDetail(p.tribeCharacters(), "EYE"),   "Occhio"),
                resourceCell("star",   countByDetail(p.tribeCharacters(), "STAR"),  "Stella"),
                resourceCell("building", p.buildings().size(),            "Edifici")
        );
        box.getChildren().addAll(header, resources);
        return box;
    }

    private Node resourceCell(String iconName, int count, String tooltip) {
        Node icon = ImageCache.icon(ImageCache.iconPath(iconName), ICON_S,
                iconName.substring(0, 1).toUpperCase(), placeholderColorForIcon(iconName));
        Label countLbl = new Label(String.valueOf(count));
        countLbl.getStyleClass().add("resource-count");
        HBox cell = new HBox(3, icon, countLbl);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("resource-cell");
        if (tooltip != null && !tooltip.isBlank()) {
            javafx.scene.control.Tooltip.install(cell, new javafx.scene.control.Tooltip(tooltip));
        }
        return cell;
    }

    private Color placeholderColorForIcon(String iconName) {
        return switch (iconName.toLowerCase()) {
            case "food"     -> Color.web("#e07b3a");
            case "stone"    -> Color.web("#7a5538");
            case "arrow"    -> Color.web("#c0392b");
            case "hand"     -> Color.web("#46a3a3");
            case "eye"      -> Color.web("#5d3f8a");
            case "star"     -> Color.web("#d4a73a");
            case "building" -> Color.web("#8a6a3f");
            default         -> Color.web("#888888");
        };
    }

    // ------ local bar (bottom) ------

    private void renderLocalBar(GameStateDTO state) {
        String me = app.getSession().getLocalNickname();
        PlayerDTO local = null;
        if (me != null) {
            for (PlayerDTO p : state.players()) {
                if (me.equalsIgnoreCase(p.nickname())) { local = p; break; }
            }
        }
        if (local == null) {
            localBar.setCenter(new Label("(spettatore)"));
            return;
        }
        localBar.setCenter(buildPlayerPanel(local, true));
    }

    // ------ action panel ------

    private void renderActionPanel(GameStateDTO state) {
        boolean myTurn = isMyTurn(state);
        String phase = state.currentPhase() == null ? "" : state.currentPhase();
        actionHint.getStyleClass().remove("action-hint-mine");
        if (!myTurn) {
            actionHint.setText("In attesa di " + state.activePlayer() + "...");
            confirmPickButton.setDisable(true);
            return;
        }
        actionHint.getStyleClass().add("action-hint-mine");
        if (isTotemPhase(phase)) {
            actionHint.setText("Tocca a te: clicca una tessera offerta libera per piazzare il totem.");
            confirmPickButton.setDisable(true);
        } else if (isActionPhase(phase)) {
            actionHint.setText("Tocca a te: seleziona le carte e premi Conferma.");
            confirmPickButton.setDisable(false);
        } else if (isBonusPhase(phase)) {
            actionHint.setText("Tocca a te: scegli UNA carta bonus e premi Conferma.");
            confirmPickButton.setDisable(false);
        } else {
            actionHint.setText("Tocca a te (" + phase + ").");
            confirmPickButton.setDisable(true);
        }
    }

    /** Deseleziona visivamente tutte le carte e svuota il set di selezione. */
    private void clearCardSelection() {
        pickedCardIds.clear();
        for (Node n : upperRowBox.getChildren()) {
            if (n instanceof ToggleButton tb) tb.setSelected(false);
        }
        for (Node n : lowerRowBox.getChildren()) {
            if (n instanceof ToggleButton tb) tb.setSelected(false);
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
                    statusLabel.setText("Seleziona esattamente una carta bonus.");
                    return;
                }
                app.getSession().getClientController().pickBonusCard(ids.get(0));
            } else {
                app.getSession().getClientController().pickCards(ids);
            }
            pickedCardIds.clear();
        } catch (RuntimeException ex) {
            statusLabel.setText("Invio fallito: " + ex.getMessage());
        }
    }

    // =====================================================================
    // helper
    // =====================================================================

    private boolean isMyTurn(GameStateDTO state) {
        String me = app.getSession().getLocalNickname();
        return me != null && me.equalsIgnoreCase(state.activePlayer());
    }

    // Detection delle fasi tollerante alla lingua: i nomi reali del modello
    // sono in italiano (vedi *State.getPhaseName()): "Piazzamento Totem",
    // "Risoluzione Azioni", "Selezione Carta Bonus", "Risoluzione Eventi", ...
    // Manteniamo anche le parole inglesi nel caso vengano rinominate.

    private boolean isTotemPhase(String phase) {
        if (phase == null) return false;
        String p = phase.toLowerCase();
        return p.contains("totem") || p.contains("piazzamento");
    }

    private boolean isActionPhase(String phase) {
        if (phase == null) return false;
        String p = phase.toLowerCase();
        // Esclude esplicitamente la risoluzione eventi.
        if (p.contains("event") || p.contains("eventi")) return false;
        return p.contains("azion") || p.contains("action") || p.contains("resolution") || p.contains("risoluzion");
    }

    private boolean isBonusPhase(String phase) {
        return phase != null && phase.toLowerCase().contains("bonus");
    }

    private int countByDetail(List<CardDTO> cards, String detailKeyword) {
        if (cards == null || detailKeyword == null) return 0;
        int n = 0;
        for (CardDTO c : cards) {
            if (c.detailType() != null && c.detailType().toUpperCase().contains(detailKeyword)) n++;
        }
        return n;
    }

    private String totemColorForNickname(GameStateDTO state, String nickname) {
        if (nickname == null) return null;
        for (PlayerDTO p : state.players()) {
            if (nickname.equalsIgnoreCase(p.nickname())) return p.totemColor();
        }
        return null;
    }

    private String initialOf(String nickname) {
        if (nickname == null || nickname.isBlank()) return "?";
        return nickname.substring(0, 1).toUpperCase();
    }

    private Node spacer() {
        javafx.scene.layout.Region r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
}
