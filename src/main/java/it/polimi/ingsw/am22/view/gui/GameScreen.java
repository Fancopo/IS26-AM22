package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.common.dto.CardDTO;
import it.polimi.ingsw.am22.network.common.dto.GameStateDTO;
import it.polimi.ingsw.am22.network.common.dto.OfferTileDTO;
import it.polimi.ingsw.am22.network.common.dto.PlayerDTO;
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
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
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
 *     <li><b>top</b>: header con titolo della fase corrente, round, era;</li>
 *     <li><b>center</b>: board centrale → riga superiore di carte, sezione
 *         tessere offerta (immagine numplayer_N + tessere su una sola riga),
 *         riga inferiore di carte;</li>
 *     <li><b>right</b>: pannelli giocatori (TUTTI, incluso quello locale)
 *         impilati verticalmente con totem, PP, e griglia 4×3 di icone
 *         risorsa/personaggio;</li>
 *     <li><b>bottom</b>: status bar per messaggi info/errore.</li>
 * </ul>
 *
 * <p>Tutti i nodi grafici (carte, tessere, totem, icone) sono caricati via
 * {@link ImageCache}: se il PNG corrispondente esiste nei resources, viene
 * mostrato; altrimenti si vede un placeholder colorato con etichetta. Drop-in
 * dei file PNG in {@code src/main/resources/images/...} → la UI si aggiorna
 * automaticamente al riavvio.
 *
 * <p>Le azioni disponibili dipendono dalla fase, individuata per substring
 * sul campo {@code currentPhase} del DTO. La detection è bilingue
 * (italiano/inglese):
 * <ul>
 *     <li>"Piazzamento Totem" / "Totem" → click su una tessera offerta libera
 *         per piazzare il totem;</li>
 *     <li>"Risoluzione Azioni" / "Action" → selezione carte dalle due righe +
 *         Conferma;</li>
 *     <li>"Selezione Carta Bonus" / "Bonus" → selezione di una sola carta tra
 *         quelle proposte + Conferma.</li>
 * </ul>
 * I controlli sono attivi solo se {@code activePlayer} coincide col nickname locale.
 */
public final class GameScreen implements GuiScreen {

    // Dimensioni di display (raddoppiate nei PNG → vedi linee guida asset).
    private static final double CARD_W = 110;
    private static final double CARD_H = 150;
    // Tessere offerta: stessa dimensione delle carte per uniformità visiva.
    private static final double TILE_W = 110;
    private static final double TILE_H = 150;
    // Immagine "board" (numplayer_N) affiancata alle tessere offerta.
    private static final double BOARD_W = 110;
    private static final double BOARD_H = 150;
    private static final double TOTEM_S = 32;
    private static final double ICON_S = 22;

    private final GuiApp app;
    private final BorderPane root = new BorderPane();

    // Header
    private final Label headerTitle = new Label();
    private final Label roundLabel = new Label();
    private final Label eraLabel = new Label();
    private final Label phaseLabel = new Label();

    // Board centrale: HBox (NON FlowPane) → niente wrap su 2 righe.
    private final HBox upperRowBox = new HBox(8);
    private final HBox lowerRowBox = new HBox(8);
    private final HBox offerTilesBox = new HBox(8);
    /**
     * Sezione tessere offerta: immagine numplayer_N a sinistra + tessere a destra.
     * Ricostruita ad ogni render perché l'immagine cambia col numero di giocatori.
     */
    private final HBox offerSectionBox = new HBox(12);

    // Right column: TUTTI i giocatori (incluso il locale, evidenziato).
    private final VBox playersBox = new VBox(10);

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
        applyBackgroundImage();

        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setRight(buildRightColumn());
        root.setBottom(buildBottom());
    }

    /**
     * Imposta lo sfondo del BorderPane radice tutto in Java.
     *
     * <p>Tentiamo prima di caricare l'immagine {@code mesos-loading.png}; se
     * disponibile, viene applicata in modalità "cover". Se non si carica,
     * cadiamo su un gradiente rosso analogo a quello del CSS.
     *
     * <p>NOTA TECNICA: il CSS della classe {@code .game-root} è volutamente
     * vuoto. Se contenesse {@code -fx-background-color}, il CSS verrebbe
     * applicato DOPO questo metodo e sovrascriverebbe l'immagine.
     */
    private void applyBackgroundImage() {
        Image bg = ImageCache.load("/images/background/mesos-loading.png");
        if (bg != null) {
            javafx.scene.layout.BackgroundImage bgImage = new javafx.scene.layout.BackgroundImage(
                    bg,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                    javafx.scene.layout.BackgroundPosition.CENTER,
                    new javafx.scene.layout.BackgroundSize(
                            javafx.scene.layout.BackgroundSize.AUTO,
                            javafx.scene.layout.BackgroundSize.AUTO,
                            true, true, false, true /* cover */));
            root.setBackground(new javafx.scene.layout.Background(bgImage));
            return;
        }
        // Fallback: gradiente rosso (se l'immagine manca, vediamo questo).
        javafx.scene.paint.LinearGradient grad = new javafx.scene.paint.LinearGradient(
                0, 0, 0, 1, true, javafx.scene.paint.CycleMethod.NO_CYCLE,
                new javafx.scene.paint.Stop(0.0,  Color.web("#5a0f1a")),
                new javafx.scene.paint.Stop(0.45, Color.web("#8a1c20")),
                new javafx.scene.paint.Stop(1.0,  Color.web("#c43a1f")));
        root.setBackground(new javafx.scene.layout.Background(
                new javafx.scene.layout.BackgroundFill(grad,
                        javafx.scene.layout.CornerRadii.EMPTY,
                        javafx.geometry.Insets.EMPTY)));
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
        offerSectionBox.setAlignment(Pos.CENTER);
        upperRowBox.getStyleClass().add("board-row");
        lowerRowBox.getStyleClass().add("board-row");
        offerSectionBox.getStyleClass().add("board-row");

        Label upLbl = new Label("Riga superiore");
        Label lowLbl = new Label("Riga inferiore");
        Label tileLbl = new Label("Tessere offerta");
        for (Label l : new Label[]{upLbl, lowLbl, tileLbl}) {
            l.getStyleClass().add("board-section-label");
        }

        // VBox interna che impila le sezioni con allineamento centrato.
        // Il "tracciato turni" è stato rimosso dalla GUI: l'informazione resta
        // nel DTO (state.turnOrder()) ed è ancora mostrata dal TUI.
        VBox center = new VBox(10,
                upLbl, upperRowBox,
                tileLbl, offerSectionBox,
                lowLbl, lowerRowBox);
        center.setPadding(new Insets(14));
        center.setAlignment(Pos.TOP_CENTER);
        center.setFillWidth(false);

        // Wrapper che centra orizzontalmente E verticalmente la VBox.
        StackPane centerWrap = new StackPane(center);
        StackPane.setAlignment(center, Pos.CENTER);
        centerWrap.setStyle("-fx-background-color: transparent;");

        ScrollPane scroll = new ScrollPane(centerWrap);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
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
        // Solo la status bar in basso: tutti i pannelli giocatore sono a destra.
        statusLabel.getStyleClass().add("status-bar");
        return statusLabel;
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
        renderPlayers(state);
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
     * Path atteso: {@code /images/cards/card_{id}.png}.
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

    // ------ offer tiles + numplayer board ------

    private void renderOfferTiles(GameStateDTO state) {
        // (1) Riempie l'HBox interno delle tessere.
        offerTilesBox.getChildren().clear();
        boolean myTurn = isMyTurn(state);
        boolean totemPhase = isTotemPhase(state.currentPhase());
        for (OfferTileDTO t : state.offerTrack()) {
            offerTilesBox.getChildren().add(buildOfferTileNode(t, state, myTurn && totemPhase));
        }

        // (2) Compone la sezione: a sinistra l'immagine numplayer_N, a destra
        // le tessere. L'immagine cambia solo col numero di giocatori; provia-
        // mo prima .png poi .jpg perché le risorse hanno entrambe le estensioni
        // (es. numplayer_4.jpg).
        offerSectionBox.getChildren().clear();
        int n = state.players() == null ? 0 : state.players().size();
        Node board = ImageCache.nodeFirst(BOARD_W, BOARD_H,
                "Board " + n + "p", Color.web("#5a2a2a"),
                "/images/board/numplayer_" + n + ".png",
                "/images/board/numplayer_" + n + ".jpg");
        offerSectionBox.getChildren().addAll(board, offerTilesBox);
    }

    /**
     * Costruisce una tessera offerta cliccabile. PNG atteso:
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

    // ------ players (right column) ------

    /**
     * Renderizza TUTTI i giocatori nella colonna destra, uno sotto l'altro.
     * Il giocatore locale viene messo per primo e marcato con {@code local=true}:
     * eredita lo stile {@code .local-player-panel} (sfondo distintivo, ma
     * SENZA glow dorato). Il glow appare solo per il giocatore di turno
     * tramite la classe {@code .player-panel-active}.
     */
    private void renderPlayers(GameStateDTO state) {
        playersBox.getChildren().clear();
        String me = app.getSession().getLocalNickname();

        PlayerDTO local = null;
        List<PlayerDTO> others = new ArrayList<>();
        for (PlayerDTO p : state.players()) {
            if (me != null && me.equalsIgnoreCase(p.nickname())) local = p;
            else others.add(p);
        }
        if (local != null) playersBox.getChildren().add(buildPlayerPanel(local, true));
        for (PlayerDTO p : others) {
            playersBox.getChildren().add(buildPlayerPanel(p, false));
        }
    }

    private Node buildPlayerPanel(PlayerDTO p, boolean local) {
        VBox box = new VBox(6);
        // Tutti i pannelli usano lo stesso stile base. .local-player-panel
        // distingue il pannello locale (sfondo caldo + bordo crema sottile,
        // niente glow). .player-panel-active aggiunge il glow dorato SOLO
        // quando è il turno di quel giocatore.
        box.getStyleClass().add("player-panel");
        if (local) box.getStyleClass().add("local-player-panel");
        if (p.active()) box.getStyleClass().add("player-panel-active");

        Node totem = ImageCache.icon(ImageCache.totemPath(p.totemColor()), TOTEM_S,
                initialOf(p.nickname()), ImageCache.colorFromName(p.totemColor()));

        Label name = new Label(p.nickname() + (local ? " (tu)" : ""));
        name.getStyleClass().add("player-name");
        Label pp = new Label(p.prestigePoints() + " ★");
        pp.getStyleClass().add("player-pp");

        HBox header = new HBox(8, totem, name, spacer(), pp);
        header.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(header, buildResourceGrid(p));
        return box;
    }

    /**
     * Griglia 4×3 di icone risorsa/personaggio. La mappatura icona → conteggio
     * è centralizzata nell'array {@code specs}: per cambiare ordine o significato
     * di una cella basta editare quell'array.
     *
     * <p>Naming convention: per ogni {@code iconName}, il path atteso è
     * {@code /images/icons/icon_{iconName}.png} (vedi {@link ImageCache#iconPath}).
     */
    private Node buildResourceGrid(PlayerDTO p) {
        ResourceSpec[] specs = new ResourceSpec[] {
                // Riga 1: risorse generali
                new ResourceSpec("food",                p.food(),                         "Cibo"),
                new ResourceSpec("star",                p.prestigePoints(),               "Punti Prestigio"),
                new ResourceSpec("characters_count",    p.tribeCharacters().size(),       "Personaggi totali"),

                // Riga 2: bonus / sconti / icone uniche
                // Note: building_discount e gatherer_discount NON sono esposti dal
                // PlayerDTO, quindi mostrano 0. Per popolarli serve estendere il DTO.
                new ResourceSpec("building_discount",   0,                                "Sconto edifici (Builder)"),
                new ResourceSpec("gatherer_discount",   0,                                "Sconto Gatherer"),
                new ResourceSpec("inventor_icons",      countUniqueInventorIcons(p),      "Icone uniche Inventor"),

                // Riga 3: personaggi tribù — gruppo 1
                new ResourceSpec("artist",   countByDetail(p.tribeCharacters(), "ARTIST"),    "Artisti"),
                new ResourceSpec("builder",  countByDetail(p.tribeCharacters(), "BUILDER"),   "Costruttori"),
                new ResourceSpec("hunter",   countByDetail(p.tribeCharacters(), "HUNTER"),    "Cacciatori"),

                // Riga 4: personaggi tribù — gruppo 2
                new ResourceSpec("inventor", countByDetail(p.tribeCharacters(), "INVENTOR"),  "Inventori"),
                new ResourceSpec("shaman",   countByDetail(p.tribeCharacters(), "SHAMAN"),    "Sciamani"),
                new ResourceSpec("gatherer", countByDetail(p.tribeCharacters(), "COLLECTOR"), "Raccoglitori"),
        };

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(4);
        grid.getStyleClass().add("resource-grid");
        for (int i = 0; i < specs.length; i++) {
            int col = i % 3;
            int row = i / 3;
            grid.add(resourceCell(specs[i]), col, row);
        }
        return grid;
    }

    /** Spec di una cella della griglia risorse. */
    private record ResourceSpec(String iconName, int count, String tooltip) {}

    private Node resourceCell(ResourceSpec s) {
        Node icon = ImageCache.icon(ImageCache.iconPath(s.iconName()), ICON_S,
                s.iconName().substring(0, 1).toUpperCase(),
                placeholderColorForIcon(s.iconName()));
        Label countLbl = new Label(String.valueOf(s.count()));
        countLbl.getStyleClass().add("resource-count");
        HBox cell = new HBox(4, icon, countLbl);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("resource-cell");
        if (s.tooltip() != null && !s.tooltip().isBlank()) {
            javafx.scene.control.Tooltip.install(cell, new javafx.scene.control.Tooltip(s.tooltip()));
        }
        return cell;
    }

    /**
     * Conta le icone uniche degli Inventor della tribù, basandosi sulla
     * convenzione {@code "INVENTOR-X"} usata da {@code Inventor#cardDetailType()}.
     * Replica la logica di {@code Tribe#countUniqueInventorIcons()} sul DTO.
     */
    private int countUniqueInventorIcons(PlayerDTO p) {
        if (p.tribeCharacters() == null) return 0;
        java.util.Set<String> set = new java.util.HashSet<>();
        for (CardDTO c : p.tribeCharacters()) {
            String d = c.detailType();
            if (d != null && d.toUpperCase().startsWith("INVENTOR-") && d.length() > "INVENTOR-".length()) {
                set.add(d.substring("INVENTOR-".length()));
            }
        }
        return set.size();
    }

    private Color placeholderColorForIcon(String iconName) {
        return switch (iconName.toLowerCase()) {
            case "food"               -> Color.web("#e07b3a");
            case "star"               -> Color.web("#d4a73a");
            case "characters_count"   -> Color.web("#7a5538");
            case "building_discount"  -> Color.web("#8a6a3f");
            case "gatherer_discount"  -> Color.web("#7a8a3f");
            case "inventor_icons"     -> Color.web("#46a3a3");
            case "artist"             -> Color.web("#5d3f8a");
            case "builder"            -> Color.web("#9b6d3f");
            case "hunter"             -> Color.web("#c0392b");
            case "inventor"           -> Color.web("#3a82b0");
            case "shaman"             -> Color.web("#d44a8b");
            case "gatherer"           -> Color.web("#3a9b5d");
            default                   -> Color.web("#888888");
        };
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
    // "Risoluzione Azioni", "Selezione Carta Bonus", "Risoluzione Eventi".
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
