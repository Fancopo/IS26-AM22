package it.polimi.ingsw.am22.view.gui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache di immagini con fallback automatico a placeholder colorati.
 *
 * <p>Filosofia: il codice della UI chiede sempre un'immagine per un dato
 * resource path. Se il PNG esiste nei resources, viene caricato e cachato.
 * Se non esiste (o non è ancora stato fornito), viene generato un nodo
 * placeholder colorato con etichetta. Questo permette di sviluppare la UI
 * prima di avere le risorse grafiche definitive: appena il file viene
 * droppato in {@code src/main/resources/...}, al riavvio appare al posto del
 * placeholder, senza modifiche al codice.
 *
 * <p>Convenzioni di path (vedi {@code resources/images/}):
 * <ul>
 *   <li>{@code /images/cards/{id}.png}      — fronte di una carta</li>
 *   <li>{@code /images/cards/back_*.png}    — retri dei mazzi</li>
 *   <li>{@code /images/tiles/tile_X.png}    — tessera offerta letter X</li>
 *   <li>{@code /images/icons/{name}.png}    — icone risorsa/totem</li>
 *   <li>{@code /images/board/{name}.{png|jpg}} — sfondi e tracciato turni</li>
 * </ul>
 */
public final class ImageCache {

    private ImageCache() {}

    private static final java.util.Map<String, Image> CACHE = new ConcurrentHashMap<>();
    /** Sentinella: immagine richiesta ma non trovata — evita di rifare lookup. */
    private static final Image MISSING = new Image(
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgAAIAAAUAAeImBZsAAAAASUVORK5CYII=");

    /**
     * Carica un'immagine dal classpath. Restituisce {@code null} se il file
     * non esiste — il chiamante deve allora creare un placeholder.
     */
    public static Image load(String resourcePath) {
        Image cached = CACHE.get(resourcePath);
        if (cached == MISSING) return null;
        if (cached != null) return cached;
        try {
            var url = ImageCache.class.getResource(resourcePath);
            if (url == null) {
                CACHE.put(resourcePath, MISSING);
                return null;
            }
            Image img = new Image(url.toExternalForm(), false);
            if (img.isError()) {
                CACHE.put(resourcePath, MISSING);
                return null;
            }
            CACHE.put(resourcePath, img);
            return img;
        } catch (Exception ex) {
            CACHE.put(resourcePath, MISSING);
            return null;
        }
    }

    /**
     * Restituisce un nodo che mostra l'immagine alla dimensione richiesta.
     * Se il PNG non esiste, viene restituito un placeholder colorato con
     * l'etichetta passata: drop-in dei file PNG → la UI si aggiorna da sola.
     */
    public static Node node(String resourcePath, double w, double h, String fallbackLabel, Color fallbackColor) {
        Image img = load(resourcePath);
        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(w);
            iv.setFitHeight(h);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            return iv;
        }
        return placeholder(w, h, fallbackLabel, fallbackColor);
    }

    /** Variante per icone quadrate piccole con etichetta = primo carattere. */
    public static Node icon(String resourcePath, double size, String fallbackLabel, Color fallbackColor) {
        return node(resourcePath, size, size, fallbackLabel, fallbackColor);
    }

    /** Crea un rettangolo colorato con etichetta centrata; usato come fallback. */
    public static Node placeholder(double w, double h, String label, Color color) {
        StackPane sp = new StackPane();
        sp.setPrefSize(w, h);
        sp.setMinSize(w, h);
        sp.setMaxSize(w, h);
        sp.setBackground(new Background(new BackgroundFill(
                color, new CornerRadii(6), null)));
        sp.setBorder(new Border(new BorderStroke(
                color.darker(), BorderStrokeStyle.SOLID,
                new CornerRadii(6), new BorderWidths(1.5))));
        if (label != null && !label.isBlank()) {
            Text t = new Text(label);
            t.setFill(pickReadableTextColor(color));
            t.setFont(Font.font(null, FontWeight.BOLD, Math.min(14, Math.max(9, w / 8))));
            t.setTextAlignment(TextAlignment.CENTER);
            t.setWrappingWidth(w - 8);
            sp.getChildren().add(t);
            StackPane.setAlignment(t, Pos.CENTER);
        }
        return sp;
    }

    private static Color pickReadableTextColor(Color bg) {
        double l = 0.299 * bg.getRed() + 0.587 * bg.getGreen() + 0.114 * bg.getBlue();
        return l > 0.55 ? Color.web("#1a1a1a") : Color.WHITE;
    }

    // -------------------- helper di alto livello --------------------

    /** Path della carta: {@code /images/cards/card_{id}.png}. */
    public static String cardPath(String cardId) {
        return "/images/cards/card_" + cardId + ".png";
    }

    /** Path della tessera offerta: {@code /images/tiles/tile_X.png}. */
    public static String tilePath(char letter) {
        return "/images/tiles/tile_" + Character.toUpperCase(letter) + ".png";
    }

    /** Path icona generica: {@code /images/icons/icon_{name}.png}. */
    public static String iconPath(String name) {
        return "/images/icons/icon_" + name.toLowerCase() + ".png";
    }

    /** Path icona totem giocatore: {@code /images/icons/totem_{color}.png}. */
    public static String totemPath(String color) {
        return "/images/icons/totem_" + (color == null ? "gray" : color.toLowerCase()) + ".png";
    }

    /**
     * Variante di {@link #node} che prova più path in ordine: il primo che
     * carica vince. Utile per asset che possono essere {@code .png} o
     * {@code .jpg} (es. {@code numplayer_4.jpg}).
     */
    public static Node nodeFirst(double w, double h, String fallbackLabel,
                                 Color fallbackColor, String... resourcePaths) {
        for (String p : resourcePaths) {
            Image img = load(p);
            if (img != null) {
                ImageView iv = new ImageView(img);
                iv.setFitWidth(w);
                iv.setFitHeight(h);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                return iv;
            }
        }
        return placeholder(w, h, fallbackLabel, fallbackColor);
    }

    /** Colore di placeholder a partire da una stringa colore (es. "yellow"). */
    public static Color colorFromName(String name) {
        if (name == null) return Color.GRAY;
        try {
            return Color.web(name.toLowerCase());
        } catch (Exception ex) {
            return Color.GRAY;
        }
    }
}
