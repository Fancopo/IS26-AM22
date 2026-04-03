module it.polimi.ingsw.am22 {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;


    opens it.polimi.ingsw.am22 to javafx.fxml;
    exports it.polimi.ingsw.am22;
}