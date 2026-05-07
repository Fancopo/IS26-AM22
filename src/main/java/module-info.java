module it.polimi.ingsw.am {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires java.rmi;
    requires java.sql;

    // ----------------------------------------------------------------------
    // Model
    // ----------------------------------------------------------------------
    exports it.polimi.ingsw.am22.model;
    opens it.polimi.ingsw.am22.model to javafx.fxml, javafx.graphics;

    exports it.polimi.ingsw.am22.model.states;
    opens it.polimi.ingsw.am22.model.states to javafx.fxml;

    // ----------------------------------------------------------------------
    // View
    // ----------------------------------------------------------------------
    exports it.polimi.ingsw.am22.view.gui;
    opens it.polimi.ingsw.am22.view.gui to javafx.graphics, javafx.fxml;

    exports it.polimi.ingsw.am22.view.tui;

    // ----------------------------------------------------------------------
    // Network — client side
    // ----------------------------------------------------------------------
    exports it.polimi.ingsw.am22.network.client;
    opens it.polimi.ingsw.am22.network.client to javafx.fxml, javafx.graphics, java.rmi;

    // ----------------------------------------------------------------------
    // Network — server side
    // RMI ha bisogno di "vedere" e fare reflection sulle interfacce Remote
    // e sulle implementazioni che le esportano (UnicastRemoteObject).
    // ----------------------------------------------------------------------
    exports it.polimi.ingsw.am22.network.server;
    exports it.polimi.ingsw.am22.network.server.rmi;
    opens it.polimi.ingsw.am22.network.server.rmi to java.rmi;

    exports it.polimi.ingsw.am22.network.server.socket;

    // ----------------------------------------------------------------------
    // Network — common (DTO + messaggi)
    // I record/DTO viaggiano serializzati: java.rmi deve poterli istanziare
    // tramite reflection.
    // ----------------------------------------------------------------------
    exports it.polimi.ingsw.am22.network.common.message;
    opens it.polimi.ingsw.am22.network.common.message to java.rmi;

    exports it.polimi.ingsw.am22.network.common.message.request;
    opens it.polimi.ingsw.am22.network.common.message.request to java.rmi;

    exports it.polimi.ingsw.am22.network.common.message.response;
    opens it.polimi.ingsw.am22.network.common.message.response to java.rmi;

    exports it.polimi.ingsw.am22.network.common.dto;
    opens it.polimi.ingsw.am22.network.common.dto to java.rmi;
}
