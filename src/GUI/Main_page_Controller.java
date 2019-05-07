package GUI;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Pair;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.regex.Pattern;

import static GUI.Main.*;

public class Main_page_Controller {
    @FXML
    public TextField pisanie;
    @FXML
    public TextArea chat;
    @FXML
    public ListView<String> listView;
    @FXML
    public Label nickname;
    @FXML
    public ImageView avatar;
    @FXML
    public Label avatarHelpText;
    private ObservableList<String> items = FXCollections.observableArrayList();
    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
    @FXML
    public void rText(String text) {
        Platform.runLater(() -> {
            int index = 0;
            for (int i = 0; i < text.length(); i++) {
                if (text.charAt(i) == (char) 0)
                    index++;
            }
            if (index != text.length()) {
                if (text.charAt(index) == (char) 4) {
                    System.out.println("Otrzymano komendę: " + text);
                    String id = text.substring(index + 4);

                    if (text.startsWith("CON", index + 1)) {
                        items.add(id);
                        listView.setItems(items);
                    } else if (text.startsWith("DIS", index + 1)) {
                        items.remove(id);
                        listView.setItems(items);
                    } else if (text.startsWith("ROZ", index + 1)) {
                        connectedWith = "";
                        udp_out.close();
                    } else if (text.startsWith("CAL", index + 1)) {
                        connectedWith = id;
                        udp_out.start();
                    }
                } else {
                    chat.appendText(connectedWith + ": " + text + "\n");
                }
            }
        }
        );
    }

    @FXML
    public void setAvatar() {
        System.out.println("test");
        Stage newStage = new Stage();
        final FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(newStage);
            if (file != null) {
                Image image1 = new Image(file.toURI().toString());
                avatar.setImage(image1);
                avatarHelpText.setText("");
            }
    }
    @FXML
    public void wyslij_wiadomosc() {
        pisanie.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER && !pisanie.getText().equals("") && nick != null) {
                chat.appendText(nick + ": " + pisanie.getText() + "\n");
                pol_out.setWiadomosc_wysylana(pisanie.getText());
                pisanie.clear();
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        });
    }

    private String connectedWith = "";

    @FXML
    public void connect() {
        String s = listView.getSelectionModel().getSelectedItem();
        if (s != null) {
            System.out.println(s);
            pol_out.setWiadomosc_wysylana(String.valueOf((char) 4) + "POL" + s);
            connectedWith = s;
            System.out.println("Wysłano: " + String.valueOf((char) 4) + "POL" + s);
            udp_out.start();
            synchronized (monitor) {
                monitor.notify();
            }
        }
    }

    @FXML
    public void disconnect() {
        if (!connectedWith.equals("")) {
            pol_out.setWiadomosc_wysylana(String.valueOf((char) 4) + "ROZ" + connectedWith);
            System.out.println("Wysłano: " + String.valueOf((char) 4) + "ROZ" + connectedWith);
            connectedWith = "";
            synchronized (monitor) {
                monitor.notify();
            }
        }
    }

    public void showDialog(String message) {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Połącz");

        // Set the button types.
        ButtonType loginButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        GridPane gridPane = new GridPane();
        gridPane.setHgap(10);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(20, 150, 10, 10));

        TextField ipAddress = new TextField();
        ipAddress.setPromptText("192.168.0.1");
        TextField nickName = new TextField();
        nickName.setPromptText("nickname");
        gridPane.add(new Label(message), 1, 0);
        gridPane.add(new Label("Adres ip"), 0, 1);
        gridPane.add(new Label("Nick"), 0, 2);
        gridPane.add(ipAddress, 1, 1);
        gridPane.add(nickName, 1, 2);

        dialog.getDialogPane().setContent(gridPane);

        // Request focus on the username field by default.
        Platform.runLater(() -> ipAddress.requestFocus());

        // Convert the result to a username-password-pair when the login button is clicked.
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return new Pair<>(ipAddress.getText(), nickName.getText());
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(res -> {
            nick = res.getValue();
            if (PATTERN.matcher(res.getKey()).matches()) {
                ip = res.getKey();
            } else {
                try {
                    InetAddress domainAddress = InetAddress.getByName(res.getKey());
                    ip = domainAddress.getHostAddress();
                } catch (UnknownHostException e) {
                    showDialog("Bledny adres ip, sprobuj ponownie");
                }
            }
        });
    }
}
