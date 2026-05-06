package it.polimi.ingsw.am22.view.gui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/**
 * Helper per installare uno sfondo a immagine in uno {@link StackPane}.
 *
 * <p>L'{@link ImageView} viene aggiunto come primo figlio (sotto a tutti gli
 * altri nodi) e ridimensionato per riempire l'intero contenitore.
 * Se la risorsa non viene trovata il metodo non fa nulla.
 */
final class Backgrounds {

    /** Path della risorsa di sfondo condivisa dalle schermate pre-partita. */
    static final String DEFAULT = "/images/background.png";

    private Backgrounds() {
    }

    /** Installa {@link #DEFAULT} come primo figlio di {@code container}. */
    static void install(StackPane container) {
        install(container, DEFAULT);
    }

    /** Installa la risorsa indicata come primo figlio di {@code container}. */
    static void install(StackPane container, String resourcePath) {
        var url = Backgrounds.class.getResource(resourcePath);
        if (url == null) return;
        ImageView bg = new ImageView(new Image(url.toExternalForm()));
        bg.setPreserveRatio(false);
        bg.fitWidthProperty().bind(container.widthProperty());
        bg.fitHeightProperty().bind(container.heightProperty());
        container.getChildren().add(0, bg);
    }

    /**
     * Stile pannello traslucido usato per migliorare la leggibilità dei testi
     * sopra lo sfondo rosso scuro. Applicalo a un contenitore (tipicamente un
     * VBox) che raggruppa label/controlli di una schermata.
     */
    static final String PANEL_STYLE =
            "-fx-background-color: rgba(255,255,255,0.85);"
                    + " -fx-background-radius: 12;"
                    + " -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 16, 0.2, 0, 4);";

    /** Applica {@link #PANEL_STYLE} e un padding interno alla regione. */
    static void stylePanel(Region panel) {
        panel.setStyle(PANEL_STYLE);
        panel.setPadding(new javafx.geometry.Insets(24));
    }
}
