module it.polimi.ingsw.am {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.rmi;

    // Package principali: esportati e aperti ai moduli JavaFX per consentire
    // la reflection che Application.launch e FXMLLoader eseguono sulle classi
    // pubbliche (es. ClientApp, GuiApp e schermate).
    exports it.polimi.ingsw.am22;
    opens it.polimi.ingsw.am22 to javafx.fxml, javafx.graphics;

    exports it.polimi.ingsw.am22.model;
    opens it.polimi.ingsw.am22.model to javafx.fxml, javafx.graphics;

    exports it.polimi.ingsw.am22.model.states;
    opens it.polimi.ingsw.am22.model.states to javafx.fxml;

    // View JavaFX: aperta a javafx.graphics per la launch della Application.
    exports it.polimi.ingsw.am22.view.gui;
    opens it.polimi.ingsw.am22.view.gui to javafx.graphics, javafx.fxml;

    // View TUI: normale export (non serve reflection da JavaFX).
    exports it.polimi.ingsw.am22.view.tui;
    exports it.polimi.ingsw.am22.network.client;
    opens it.polimi.ingsw.am22.network.client to javafx.fxml, javafx.graphics;
}
