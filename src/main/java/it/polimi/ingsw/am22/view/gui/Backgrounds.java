package it.polimi.ingsw.am22.view.gui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Helper for installing a background image and styling translucent panels in a StackPane. */
final class Backgrounds {

    static final String DEFAULT = "/images/background.png";

    static final String PANEL_STYLE =
            "-fx-background-color: rgba(255,255,255,0.85);"
                    + " -fx-background-radius: 12;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0.2, 0, 4);";

    private Backgrounds() {}

    static void install(StackPane container) {
        install(container, DEFAULT);
    }

    /** Installs the resource as the first child of {@code container}; no-op if the resource is missing. */
    static void install(StackPane container, String resourcePath) {
        var url = Backgrounds.class.getResource(resourcePath);
        if (url == null) return;
        ImageView bg = new ImageView(new Image(url.toExternalForm()));
        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(container.widthProperty());
        bg.fitHeightProperty().bind(container.heightProperty());
        container.getChildren().add(0, bg);
    }

    static void stylePanel(Region panel) {
        panel.setStyle(PANEL_STYLE);
        panel.setPadding(new javafx.geometry.Insets(24));
    }

    /** Styles {@code panel}, caps its width, and wraps it in a {@link StackPane} with the default background. */
    static StackPane wrapInPanel(Region panel, double maxWidth, String screenId) {
        return wrapInPanel(panel, maxWidth, screenId, DEFAULT);
    }

    /** Styles {@code panel}, caps its width, and wraps it in a {@link StackPane} with {@code backgroundResource}. */
    static StackPane wrapInPanel(Region panel, double maxWidth, String screenId, String backgroundResource) {
        stylePanel(panel);
        panel.setMaxWidth(maxWidth);
        panel.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane container = new StackPane(panel);
        container.setId(screenId);
        install(container, backgroundResource);
        return container;
    }
}
