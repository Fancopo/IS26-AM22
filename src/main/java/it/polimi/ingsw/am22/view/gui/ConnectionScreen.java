package it.polimi.ingsw.am22.view.gui;

import it.polimi.ingsw.am22.network.client.ConnectionFactory;
import it.polimi.ingsw.am22.network.client.ConnectionFactory.Transport;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Schermata iniziale: l'utente sceglie trasporto (Socket/RMI), host e porta,
 * poi preme "Connect". Al successo si passa alla {@link NicknameScreen}.
 *
 * <p>I campi sono nodi standard JavaFX: lo style/immagini di sfondo li
 * può applicare più avanti inserendo le risorse grafiche (PNG/CSS).
 */
public final class ConnectionScreen implements GuiScreen {

    private final GuiApp app;
    private final StackPane root;

    /**
     * Costruisce la schermata di connessione.
     * Invocata da {@link GuiApp#showConnectionScreen()} all'avvio della GUI
     * e ogni volta che si vuole tornare alla scelta del server (es. dopo
     * una disconnessione inattesa).
     */
    public ConnectionScreen(GuiApp app) {
        this.app = app;
        this.root = buildUi();
    }

    /**
     * Restituisce il nodo radice della schermata.
     * Chiamato da {@link GuiApp#setScreen} per montare questa schermata nello stage.
     */
    @Override
    public Parent getRoot() {
        return root;
    }

    /**
     * Crea l'intero layout JavaFX della schermata: radio button per il trasporto
     * (Socket/RMI), campi host e porta, pulsante "Connect" e label di stato.
     * Il pulsante "Connect" invoca {@link GuiApp#connect}: se la connessione
     * riesce si passa alla {@link NicknameScreen}.
     */
    private StackPane buildUi() {
        // Toggle per il trasporto.
        ToggleGroup transportGroup = new ToggleGroup();
        RadioButton socketRadio = new RadioButton("Socket (TCP)");
        socketRadio.setToggleGroup(transportGroup);
        socketRadio.setSelected(true);
        socketRadio.setUserData(Transport.SOCKET);
        RadioButton rmiRadio = new RadioButton("RMI");
        rmiRadio.setToggleGroup(transportGroup);
        rmiRadio.setUserData(Transport.RMI);

        TextField hostField = new TextField("127.0.0.1");
        TextField portField = new TextField(String.valueOf(ConnectionFactory.DEFAULT_SOCKET_PORT));
        // Quando cambia il trasporto aggiorniamo automaticamente la porta di default.
        transportGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            Transport t = (Transport) newT.getUserData();
            portField.setText(String.valueOf(t == Transport.SOCKET
                    ? ConnectionFactory.DEFAULT_SOCKET_PORT
                    : ConnectionFactory.DEFAULT_RMI_PORT));
        });

        Button connectButton = new Button("Connect");
        Label statusLabel = new Label();

        connectButton.setOnAction(e -> {
            statusLabel.setText("");
            Transport transport = (Transport) transportGroup.getSelectedToggle().getUserData();
            String host = hostField.getText().trim();
            if (host.isEmpty()) {
                statusLabel.setText("Host is required.");
                return;
            }
            int port;
            try {
                port = Integer.parseInt(portField.getText().trim());
            } catch (NumberFormatException ex) {
                statusLabel.setText("Port must be a number.");
                return;
            }
            // GuiApp apre la connessione; in caso di successo si passa alla nickname screen.
            connectButton.setDisable(true);
            boolean ok = app.connect(transport, host, port);
            connectButton.setDisable(false);
            if (ok) {
                app.showNicknameScreen();
            }
        });

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(20));
        form.addRow(0, new Label("Transport:"), new VBox(6, socketRadio, rmiRadio));
        form.addRow(1, new Label("Host:"), hostField);
        form.addRow(2, new Label("Port:"), portField);

        VBox box = new VBox(16,
                new Label("Welcome to MESOS"),
                form,
                connectButton,
                statusLabel);
        box.setAlignment(Pos.CENTER);
        Backgrounds.stylePanel(box);
        box.setMaxWidth(420);
        box.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);

        StackPane container = new StackPane(box);
        container.setId("connection-root");
        Backgrounds.install(container);
        return container;
    }
}
