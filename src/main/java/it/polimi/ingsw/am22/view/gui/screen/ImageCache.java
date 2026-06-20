package it.polimi.ingsw.am22.view.gui.screen;

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
 * Image cache with automatic fallback to colored placeholders.
 *
 * <p>Philosophy: UI code always asks for an image given a resource path. If
 * the PNG exists in the resources, it is loaded and cached. If it does not
 * exist (or has not been provided yet), a colored placeholder node with a
 * label is generated. This allows the UI to be developed before the final
 * graphic assets are available: as soon as the file is dropped into
 * {@code src/main/resources/...}, it appears in place of the placeholder on
 * the next start, with no code changes.
 *
 * <p>Path conventions (see {@code resources/images/}):
 * <ul>
 *   <li>{@code /images/cards/{id}.png}      — front of a card</li>
 *   <li>{@code /images/cards/back_*.png}    — deck backs</li>
 *   <li>{@code /images/tiles/tile_X.png}    — offer tile with letter X</li>
 *   <li>{@code /images/icons/{name}.png}    — resource/totem icons</li>
 *   <li>{@code /images/board/{name}.{png|jpg}} — backgrounds and turn-order track</li>
 * </ul>
 */
public final class ImageCache {

    private ImageCache() {}

    private static final java.util.Map<String, Image> CACHE = new ConcurrentHashMap<>();
    /** Sentinel: image requested but not found — avoids repeating the lookup. */
    private static final Image MISSING = new Image(
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNgAAIAAAUAAeImBZsAAAAASUVORK5CYII=");

    /**
     * Loads an image from the classpath. Returns {@code null} if the file
     * does not exist — the caller must then create a placeholder.
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
     * Returns a node showing the image at the requested size. If the PNG does
     * not exist, a colored placeholder with the given label is returned:
     * dropping in the PNG files makes the UI update by itself.
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

    /** Variant for small square icons with label = first character. */
    public static Node icon(String resourcePath, double size, String fallbackLabel, Color fallbackColor) {
        return node(resourcePath, size, size, fallbackLabel, fallbackColor);
    }

    /** Creates a colored rectangle with a centered label; used as a fallback. */
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

    // -------------------- high-level helpers --------------------

    /** Card path: {@code /images/cards/card_{id}.png}. */
    public static String cardPath(String cardId) {
        return "/images/cards/card_" + cardId + ".png";
    }

    /** Offer tile path: {@code /images/tiles/tile_X.png}. */
    public static String tilePath(char letter) {
        return "/images/tiles/tile_" + Character.toUpperCase(letter) + ".png";
    }

    /** Generic icon path: {@code /images/icons/icon_{name}.png}. */
    public static String iconPath(String name) {
        return "/images/icons/icon_" + name.toLowerCase() + ".png";
    }

    /**
     * Path of a specific Inventor icon: {@code /images/icons/InventorIcon_X.png}.
     * The file name uses the UPPERCASE letter (A, B, C, ...), so it CANNOT go
     * through {@link #iconPath(String)}, which instead forces lowercase.
     */
    public static String inventorIconPath(char icon) {
        return "/images/icons/InventorIcon_" + Character.toUpperCase(icon) + ".png";
    }

    /**
     * Path of the player's totem image. Files live in {@code /images/totem/}
     * named after the lowercase English color (e.g. {@code red.jpg}).
     */
    public static String totemPath(String color) {
        if (color == null) return "/images/totem/_missing.jpg";
        return "/images/totem/" + color.toLowerCase() + ".jpg";
    }

    /**
     * Returns a totem node at the requested size, trying {@code .png} then
     * {@code .jpg} in order, so it is enough to drop the file into the
     * {@code /images/totem/} folder regardless of the extension.
     */
    public static Node totemNode(String color, double size, String fallbackLabel) {
        if (color == null) {
            return placeholder(size, size, fallbackLabel, Color.GRAY);
        }
        String base = "/images/totem/" + color.toLowerCase();
        return nodeFirst(size, size, fallbackLabel, colorFromName(color),
                base + ".png", base + ".jpg");
    }

    /**
     * Variant of {@link #node} that tries multiple paths in order: the first
     * one that loads wins. Useful for assets that may be {@code .png} or
     * {@code .jpg} (e.g. {@code numplayer_4.jpg}).
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

    /** Placeholder color from a color string (e.g. "yellow"). */
    public static Color colorFromName(String name) {
        if (name == null) return Color.GRAY;
        try {
            return Color.web(name.toLowerCase());
        } catch (Exception ex) {
            return Color.GRAY;
        }
    }
}
