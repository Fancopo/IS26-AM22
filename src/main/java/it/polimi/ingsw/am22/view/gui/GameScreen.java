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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
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
 *     <li><b>center</b>: board centrale → riga superiore di carte raggruppate
 *         per categoria, sezione tessere offerta (immagine numplayer_N + tessere
 *         su una sola riga), riga inferiore di carte raggruppate per categoria;</li>
 *     <li><b>right</b>: pannelli giocatori (TUTTI, incluso quello locale)
 *         impilati verticalmente con totem, PP, griglia 4×3 di icone, e le
 *         miniature delle carte pescate fino a quel momento;</li>
 *     <li><b>bottom</b>: status bar per messaggi info/errore.</li>
 * </ul>
 *
 * <p>Le dimensioni di carte/tessere/totem sono ricalcolate ad ogni render
 * sulla base della larghezza/altezza correnti del root, e un listener
 * sulle proprietà di dimensione del root forza un re-render: in questo
 * modo l'intera scena segue la dimensione della finestra (full-screen,
 * resize, ecc.).
 */
public final class GameScreen implements GuiScreen {

    // Dimensioni di display — ricalcolate ad ogni render() in base alla
    // dimensione corrente del root, mantenendo l'aspect ratio 110:150.
    private double cardW = 110;
    private double cardH = 150;
    private double tileW = 110;
    private double tileH = 150;
    private double boardW = 110;
    private double boardH = 150;
    // Right-column / player-panel: anch'essi responsive.
    private double rightColW = 290;
    private double totemS = 32;
    private double iconS = 22;
    private double miniCardW = 60;
    private double miniCardH = 82;
    private static final double CARD_RATIO_W_OVER_H = 110.0 / 150.0;

    private final GuiApp app;
    private final BorderPane root = new BorderPane();

    // Header
    private final Label headerTitle = new Label();
    private final Label roundLabel = new Label();
    private final Label eraLabel = new Label();
    private final Label phaseLabel = new Label();

    // Board centrale: HBox (NON FlowPane) → niente wrap su 2 righe.
    private final HBox upperRowBox = new HBox(14);
    private final HBox lowerRowBox = new HBox(14);
    private final HBox offerTilesBox = new HBox(8);
    /**
     * Sezione tessere offerta: immagine numplayer_N a sinistra + tessere a destra.
     * Ricostruita ad ogni render perché l'immagine cambia col numero di giocatori.
     */
    private final HBox offerSectionBox = new HBox(12);

    // Right column: TUTTI i giocatori (incluso il locale, evidenziato).
    private final VBox playersBox = new VBox(10);
    private VBox rightColumnBox;
    private ScrollPane rightColumnScroll;

    // Action panel (dentro la colonna destra, in basso)
    private final VBox actionBox = new VBox(8);
    private final Label actionHint = new Label();
    private final Button confirmPickButton = new Button("Confirm");
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
                statusLabel.setText("Error: " + m.message());
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

        // Re-render whenever the window is resized so card / tile images
        // recompute their dimensions to fit the new viewport.
        javafx.beans.value.ChangeListener<Number> resizeListener = (obs, ov, nv) -> {
            GameStateDTO cached = app.getSession() == null ? null
                    : app.getSession().getLatestGameState();
            if (cached != null) render(cached);
        };
        root.widthProperty().addListener(resizeListener);
        root.heightProperty().addListener(resizeListener);
    }

    /**
     * Imposta lo sfondo del BorderPane radice tutto in Java.
     *
     * <p>NOTA TECNICA: il CSS della classe {@code .game-root} è volutamente
     * vuoto. Se contenesse {@code -fx-background-color}, il CSS verrebbe
     * applicato DOPO questo metodo e sovrascriverebbe l'immagine.
     */
    private void applyBackgroundImage() {
        Image bg = ImageCache.load("/images/background/background_noMESOS.png");
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
        headerTitle.setText("Waiting...");

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
        // Le righe esterne sono trasparenti: i pannelli visibili (Building/
        // Character/Event) vengono creati come HBox interne in renderCardRow().
        offerSectionBox.getStyleClass().add("board-row");

        VBox center = new VBox(10,
                upperRowBox,
                offerSectionBox,
                lowerRowBox);
        center.setPadding(new Insets(14));
        center.setAlignment(Pos.TOP_CENTER);
        center.setFillWidth(false);

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
        Label actionsLbl = new Label("Actions");
        actionsLbl.getStyleClass().add("board-section-label");
        actionBox.getChildren().addAll(actionsLbl, actionHint, confirmPickButton);

        VBox right = new VBox(12, playersBox, actionBox);
        right.setPadding(new Insets(10));
        right.setPrefWidth(rightColW);

        ScrollPane scroll = new ScrollPane(right);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        scroll.setPrefWidth(rightColW + 10);
        this.rightColumnBox = right;
        this.rightColumnScroll = scroll;
        return scroll;
    }

    private Node buildBottom() {
        statusLabel.getStyleClass().add("status-bar");
        return statusLabel;
    }

    // =====================================================================
    // rendering di uno stato
    // =====================================================================

    private void render(GameStateDTO state) {
        recomputeSizes();
        applyRightColumnSize();
        roundLabel.setText("Round " + state.currentRound());
        eraLabel.setText("Era " + state.currentEra());
        phaseLabel.setText("Phase: " + state.currentPhase());
        headerTitle.setText(headerTitleFor(state));

        // Le selezioni precedenti non hanno più senso con un nuovo stato:
        // svuotiamo PRIMA di rebuildare le card così le ToggleButton
        // partono deselezionate e lo stato visivo resta sincronizzato.
        pickedCardIds.clear();

        renderUpperRow(state);
        renderLowerRow(state);
        renderOfferTiles(state);
        renderPlayers(state);
        renderActionPanel(state);
    }

    /** Dynamic title based on phase. */
    private String headerTitleFor(GameStateDTO state) {
        if (!isMyTurn(state)) {
            return "Turn of " + (state.activePlayer() == null ? "—" : state.activePlayer());
        }
        String phase = state.currentPhase() == null ? "" : state.currentPhase();
        if (isTotemPhase(phase))   return "Choose an Offer Tile to place your totem on";
        if (isActionPhase(phase))  return "Select the cards to draw and confirm";
        if (isBonusPhase(phase))   return "Choose a bonus card";
        return "Your turn";
    }

    /**
     * Recompute card / tile / board / panel dimensions from the current root
     * size while preserving the original 110:150 aspect ratio. Called at the
     * start of each render() and on every window resize.
     */
    private void recomputeSizes() {
        double rw = root.getWidth();
        double rh = root.getHeight();
        if (rw <= 0) rw = 1600;
        if (rh <= 0) rh = 900;

        // Right column scales with window width but stays in a sane range.
        rightColW = Math.max(240, Math.min(420, rw * 0.22));
        totemS = Math.max(26, Math.min(46, rightColW / 8.5));
        iconS  = Math.max(18, Math.min(34, rightColW / 13.0));

        double panelInner = rightColW - 36;
        miniCardW = Math.max(46, Math.min(110, (panelInner - 2 * 4) / 3.0));
        miniCardH = miniCardW / CARD_RATIO_W_OVER_H;

        // Center area sizing.
        double availW = Math.max(400, rw - rightColW - 50);
        double maxByWidth = (availW - 5 * 8) / 6.0;

        double availH = Math.max(300, rh - 60 - 30 - 60);
        double maxByHeight = (availH - 2 * 10) / 3.0;

        double w = Math.min(maxByWidth, maxByHeight * CARD_RATIO_W_OVER_H);
        w = Math.max(70, Math.min(w, 220));
        double h = w / CARD_RATIO_W_OVER_H;

        cardW = w;  cardH = h;
        tileW = w;  tileH = h;
        boardW = w; boardH = h;
    }

    /** Apply responsive sizes to the right column wrappers. */
    private void applyRightColumnSize() {
        if (rightColumnBox != null) rightColumnBox.setPrefWidth(rightColW);
        if (rightColumnScroll != null) {
            rightColumnScroll.setPrefWidth(rightColW + 10);
            rightColumnScroll.setMinWidth(rightColW + 10);
        }
        actionHint.setMaxWidth(rightColW - 30);
    }

    // ------ board rows ------

    private void renderUpperRow(GameStateDTO state) {
        upperRowBox.getChildren().clear();
        boolean canPick = isMyTurn(state) && (isActionPhase(state.currentPhase()) || isBonusPhase(state.currentPhase()));
        renderCardRow(upperRowBox, state.upperRow(), canPick, "UP");
    }

    private void renderLowerRow(GameStateDTO state) {
        lowerRowBox.getChildren().clear();
        boolean canPick = isMyTurn(state) && (isActionPhase(state.currentPhase()) || isBonusPhase(state.currentPhase()));
        renderCardRow(lowerRowBox, state.lowerRow(), canPick, "LOW");
    }

    /**
     * Group the cards in a board row into Building / Character / Event panels
     * (in that order) and render each non-empty group inside its own
     * {@code .board-row} HBox.
     */
    private void renderCardRow(HBox row, List<CardDTO> cards, boolean canPick, String fallback) {
        List<CardDTO> buildings  = new ArrayList<>();
        List<CardDTO> characters = new ArrayList<>();
        List<CardDTO> events     = new ArrayList<>();
        List<CardDTO> others     = new ArrayList<>();
        for (CardDTO c : cards) {
            String cat = c.category() == null ? "" : c.category().toUpperCase();
            switch (cat) {
                case "BUILDING"  -> buildings.add(c);
                case "CHARACTER" -> characters.add(c);
                case "EVENT"     -> events.add(c);
                default          -> others.add(c);
            }
        }
        addCategoryGroup(row, buildings,  canPick, fallback);
        addCategoryGroup(row, characters, canPick, fallback);
        addCategoryGroup(row, events,     canPick, fallback);
        addCategoryGroup(row, others,     canPick, fallback);
    }

    private void addCategoryGroup(HBox parent, List<CardDTO> cards, boolean canPick, String fallback) {
        if (cards.isEmpty()) return;
        HBox group = new HBox(8);
        group.setAlignment(Pos.CENTER);
        group.getStyleClass().add("board-row");
        for (CardDTO c : cards) {
            group.getChildren().add(buildCardNode(c, canPick, fallback));
        }
        parent.getChildren().add(group);
    }

    /**
     * Costruisce un nodo "carta": ToggleButton con un'immagine (o placeholder).
     */
    private ToggleButton buildCardNode(CardDTO c, boolean clickable, String fallbackLabel) {
        Color color = colorForCardCategory(c.category());
        String label = (c.id() == null ? fallbackLabel : c.id())
                + (c.detailType() == null ? "" : "\n" + c.detailType());
        Node graphic = ImageCache.node(ImageCache.cardPath(c.id()), cardW, cardH, label, color);

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
        offerTilesBox.getChildren().clear();
        boolean myTurn = isMyTurn(state);
        boolean totemPhase = isTotemPhase(state.currentPhase());
        for (OfferTileDTO t : state.offerTrack()) {
            offerTilesBox.getChildren().add(buildOfferTileNode(t, state, myTurn && totemPhase));
        }

        offerSectionBox.getChildren().clear();
        offerSectionBox.getChildren().addAll(buildNumPlayerBoard(state), offerTilesBox);
    }

    /**
     * Build the numplayer_N board with totem overlays placed on each white
     * square according to the turn-order positions.
     */
    private Node buildNumPlayerBoard(GameStateDTO state) {
        int n = state.players() == null ? 0 : state.players().size();

        Image bg = ImageCache.load("/images/board/numplayer_" + n + ".png");
        if (bg == null) bg = ImageCache.load("/images/board/numplayer_" + n + ".jpg");
        Node bgNode;
        if (bg != null) {
            ImageView iv = new ImageView(bg);
            iv.setFitWidth(boardW);
            iv.setFitHeight(boardH);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            bgNode = iv;
        } else {
            bgNode = ImageCache.placeholder(boardW, boardH,
                    "Board " + n + "p", Color.web("#5a2a2a"));
        }

        Pane overlay = new Pane();
        overlay.setPrefSize(boardW, boardH);
        overlay.setMinSize(boardW, boardH);
        overlay.setMaxSize(boardW, boardH);
        overlay.setMouseTransparent(true);

        if (state.turnOrder() != null && !state.turnOrder().isEmpty()) {
            double[] yRatios = numplayerYRatios(n);
            double xRatio = 0.50;
            // Use the same totem fit-box as on the offer tiles, so the totem
            // appears identical when it moves between the two boards.
            double sqW = 0.40 * boardW;
            double sqH = 0.18 * boardH;
            for (int i = 0; i < state.turnOrder().size() && i < yRatios.length; i++) {
                TurnSlotDTO slot = state.turnOrder().get(i);
                if (slot.occupiedBy() == null) continue;
                String color = totemColorForNickname(state, slot.occupiedBy());
                Node totemBox = makeTotemForSquare(color, sqW, sqH,
                        initialOf(slot.occupiedBy()));
                double cx = xRatio * boardW;
                double cy = yRatios[i] * boardH;
                totemBox.setLayoutX(cx - sqW / 2.0);
                totemBox.setLayoutY(cy - sqH / 2.0);
                overlay.getChildren().add(totemBox);
            }
        }

        StackPane stack = new StackPane(bgNode, overlay);
        stack.setMinSize(boardW, boardH);
        stack.setPrefSize(boardW, boardH);
        stack.setMaxSize(boardW, boardH);
        return stack;
    }

    /**
     * Vertical-center ratio (y / boardH) of each white square on the
     * {@code numplayer_N.png} board image. Indexed by turn-order position.
     */
    private static double[] numplayerYRatios(int n) {
        return switch (n) {
            case 2 -> new double[]{0.235, 0.430};
            case 3 -> new double[]{0.215, 0.370, 0.530};
            case 4 -> new double[]{0.180, 0.330, 0.490, 0.650};
            case 5 -> new double[]{0.165, 0.300, 0.450, 0.595, 0.740};
            default -> new double[0];
        };
    }

    /**
     * Build a totem node sized to fit exactly into a {@code sqW × sqH}
     * rectangle (typically a white square on a tile or board).
     */
    private Node makeTotemForSquare(String color, double sqW, double sqH, String fallbackLabel) {
        Node img = ImageCache.totemNode(color, Math.min(sqW, sqH), fallbackLabel);
        StackPane box = new StackPane(img);
        box.setMinSize(sqW, sqH);
        box.setPrefSize(sqW, sqH);
        box.setMaxSize(sqW, sqH);
        box.setMouseTransparent(true);
        return box;
    }

    /**
     * Costruisce una tessera offerta cliccabile. Se la tessera è occupata,
     * sopra il quadrato bianco appare il totem del giocatore corrispondente.
     */
    private Node buildOfferTileNode(OfferTileDTO t, GameStateDTO state, boolean clickable) {
        String fallback = String.valueOf(t.letter())
                + "\nU+" + t.upperCardsToTake()
                + " L+" + t.lowerCardsToTake()
                + "\n+" + t.foodReward() + " food";

        // Stretch the tile image to fixed bounds so overlay coordinates
        // (the white square position) align regardless of the source ratio.
        Image img = ImageCache.load(ImageCache.tilePath(t.letter()));
        Node tileImg;
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(tileW);
            iv.setFitHeight(tileH);
            iv.setPreserveRatio(false);
            iv.setSmooth(true);
            tileImg = iv;
        } else {
            tileImg = ImageCache.placeholder(tileW, tileH, fallback, Color.web("#c69862"));
        }

        StackPane content = new StackPane(tileImg);
        content.setMinSize(tileW, tileH);
        content.setPrefSize(tileW, tileH);
        content.setMaxSize(tileW, tileH);

        if (t.occupiedBy() != null) {
            String color = totemColorForNickname(state, t.occupiedBy());
            // White square on every tile_X.png is approximately 0.40 × 0.18
            // centered horizontally at 0.50, vertically at 0.22.
            double sqW = 0.40 * tileW;
            double sqH = 0.18 * tileH;
            Node totemBox = makeTotemForSquare(color, sqW, sqH,
                    initialOf(t.occupiedBy()));
            Pane overlay = new Pane();
            overlay.setPrefSize(tileW, tileH);
            overlay.setMinSize(tileW, tileH);
            overlay.setMaxSize(tileW, tileH);
            overlay.setMouseTransparent(true);
            double cx = 0.50 * tileW;
            double cy = 0.22 * tileH;
            totemBox.setLayoutX(cx - sqW / 2.0);
            totemBox.setLayoutY(cy - sqH / 2.0);
            overlay.getChildren().add(totemBox);
            content.getChildren().add(overlay);
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
                statusLabel.setText("Placement failed: " + ex.getMessage());
            }
        });
        return b;
    }

    // ------ players (right column) ------

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
        box.getStyleClass().add("player-panel");
        if (local) box.getStyleClass().add("local-player-panel");
        if (p.active()) box.getStyleClass().add("player-panel-active");

        Node totem = ImageCache.totemNode(p.totemColor(), totemS, initialOf(p.nickname()));

        Label name = new Label(p.nickname() + (local ? " (you)" : ""));
        name.getStyleClass().add("player-name");
        Label pp = new Label(p.prestigePoints() + " ★");
        pp.getStyleClass().add("player-pp");

        HBox header = new HBox(8, totem, name, spacer(), pp);
        header.setAlignment(Pos.CENTER_LEFT);

        box.getChildren().addAll(header, buildResourceGrid(p));

        Node drawnCards = buildDrawnCardsPane(p);
        if (drawnCards != null) box.getChildren().add(drawnCards);

        return box;
    }

    /**
     * Build a wrapping pane with mini-thumbnails of every card the player has
     * drawn (tribe characters + buildings).
     */
    private Node buildDrawnCardsPane(PlayerDTO p) {
        List<CardDTO> drawn = new ArrayList<>();
        if (p.tribeCharacters() != null) drawn.addAll(p.tribeCharacters());
        if (p.buildings() != null) drawn.addAll(p.buildings());
        if (drawn.isEmpty()) return null;

        FlowPane pane = new FlowPane(4, 4);
        pane.getStyleClass().add("drawn-cards-pane");
        pane.setPrefWrapLength(Math.max(80, rightColW - 36));
        pane.setMaxWidth(rightColW - 20);
        for (CardDTO c : drawn) {
            Color color = colorForCardCategory(c.category());
            String label = c.id() == null ? "?" : c.id();
            Node thumb = ImageCache.node(ImageCache.cardPath(c.id()),
                    miniCardW, miniCardH, label, color);
            if (c.id() != null) {
                javafx.scene.control.Tooltip.install(thumb,
                        new javafx.scene.control.Tooltip(
                                c.id() + (c.detailType() == null ? "" : " — " + c.detailType())));
            }
            pane.getChildren().add(thumb);
        }
        return pane;
    }

    /**
     * Griglia 4×3 di icone risorsa/personaggio.
     */
    private Node buildResourceGrid(PlayerDTO p) {
        ResourceSpec[] specs = new ResourceSpec[] {
                new ResourceSpec("food",                p.food(),                         "Food"),
                new ResourceSpec("star",                p.prestigePoints(),               "Prestige Points"),
                new ResourceSpec("characters_count",    p.tribeCharacters().size(),       "Total characters"),

                new ResourceSpec("building_discount",   0,                                "Building discount (Builder)"),
                new ResourceSpec("gatherer_discount",   0,                                "Gatherer discount"),
                new ResourceSpec("inventor_icons",      countUniqueInventorIcons(p),      "Unique Inventor icons"),

                new ResourceSpec("artist",   countByDetail(p.tribeCharacters(), "ARTIST"),    "Artists"),
                new ResourceSpec("builder",  countByDetail(p.tribeCharacters(), "BUILDER"),   "Builders"),
                new ResourceSpec("hunter",   countByDetail(p.tribeCharacters(), "HUNTER"),    "Hunters"),

                new ResourceSpec("inventor", countByDetail(p.tribeCharacters(), "INVENTOR"),  "Inventors"),
                new ResourceSpec("shaman",   countByDetail(p.tribeCharacters(), "SHAMAN"),    "Shamans"),
                new ResourceSpec("gatherer", countByDetail(p.tribeCharacters(), "COLLECTOR"), "Gatherers"),
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

    private record ResourceSpec(String iconName, int count, String tooltip) {}

    private Node resourceCell(ResourceSpec s) {
        Node icon = ImageCache.icon(ImageCache.iconPath(s.iconName()), iconS,
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
            actionHint.setText("Waiting for " + state.activePlayer() + "...");
            confirmPickButton.setDisable(true);
            return;
        }
        actionHint.getStyleClass().add("action-hint-mine");
        if (isTotemPhase(phase)) {
            actionHint.setText("Your turn: click a free offer tile to place your totem.");
            confirmPickButton.setDisable(true);
        } else if (isActionPhase(phase)) {
            actionHint.setText("Your turn: select cards and press Confirm.");
            confirmPickButton.setDisable(false);
        } else if (isBonusPhase(phase)) {
            actionHint.setText("Your turn: choose ONE bonus card and press Confirm.");
            confirmPickButton.setDisable(false);
        } else {
            actionHint.setText("Your turn (" + phase + ").");
            confirmPickButton.setDisable(true);
        }
    }

    /**
     * Deseleziona visivamente tutte le carte e svuota il set di selezione.
     * Le carte sono ora annidate in HBox interne (i pannelli per categoria),
     * quindi scendiamo ricorsivamente nei figli.
     */
    private void clearCardSelection() {
        pickedCardIds.clear();
        deselectIn(upperRowBox);
        deselectIn(lowerRowBox);
    }

    private void deselectIn(Pane container) {
        for (Node n : container.getChildren()) {
            if (n instanceof ToggleButton tb) {
                tb.setSelected(false);
            } else if (n instanceof Pane p) {
                deselectIn(p);
            }
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
        } catch (RuntimeException ex) {
            statusLabel.setText("Submit failed: " + ex.getMessage());
        }
    }

    // =====================================================================
    // helper
    // =====================================================================

    private boolean isMyTurn(GameStateDTO state) {
        String me = app.getSession().getLocalNickname();
        return me != null && me.equalsIgnoreCase(state.activePlayer());
    }

    private boolean isTotemPhase(String phase) {
        if (phase == null) return false;
        String p = phase.toLowerCase();
        return p.contains("totem") || p.contains("piazzamento");
    }

    private boolean isActionPhase(String phase) {
        if (phase == null) return false;
        String p = phase.toLowerCase();
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
