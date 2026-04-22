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

    public ConnectionScreen(GuiApp app) {
        this.app = app;
        this.root = buildUi();
    }

    @Override
    public Parent getRoot() {
        return root;
    }

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
        box.setPadding(new Insets(40));

        // GRAPHIC PLACEHOLDER: questo StackPane è il punto su cui applicare lo
        // sfondo: basta aggiungere un ImageView come primo figlio o un CSS.
        StackPane container = new StackPane(box);
        container.setId("connection-root");
        return container;
    }
}
