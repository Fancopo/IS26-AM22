module it.polimi.ingsw.am {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens it.polimi.ingsw.am22 to javafx.fxml, com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am22.character to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am22.event to com.fasterxml.jackson.databind;
    opens it.polimi.ingsw.am22.Building to com.fasterxml.jackson.databind;

    exports it.polimi.ingsw.am22;
}