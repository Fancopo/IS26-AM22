module it.polimi.ingsw.am {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.rmi;


    opens it.polimi.ingsw.am22 to javafx.fxml;
    exports it.polimi.ingsw.am22.model.states;
    opens it.polimi.ingsw.am22.model.states to javafx.fxml;
    exports it.polimi.ingsw.am22.model;
    opens it.polimi.ingsw.am22.model to javafx.fxml;
}