package GUI;

import TCP_PACK.TCP_in;
import TCP_PACK.TCP_out;
import UDP_PACK.UDP_in;
import UDP_PACK.UDP_out;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;

public class Main extends Application {

    public static volatile TCP_out pol_out;
    public static volatile TCP_in pol_in;
    public static volatile UDP_out udp_out;
    public static volatile UDP_in udp_in;
    public static volatile Socket clientSocketTCP;
    public static volatile DatagramSocket clientSocketUDP;
    public final static Object monitor = new Object(); //monitor do synchronizacji
    public static Main_page_Controller controller;
    public static String ip;
    public static String nick;
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(
                "Main_page.fxml"));
        Parent root = (Parent) loader.load();
        controller = loader.getController();

        Scene newScene = new Scene(root);
        Stage newStage = new Stage();
        newStage.setScene(newScene);
        newStage.show();
        controller.showDialog("");
        controller.nickname.setText(nick);
        int port = 1239;

        try {
            clientSocketTCP = new Socket(ip, port);
            clientSocketUDP = new DatagramSocket (1241);
            pol_in = new TCP_in();
            pol_in.start();
            pol_out = new TCP_out();
            pol_out.start();
            udp_out = new UDP_out();
            udp_in = new UDP_in();
            udp_in.start();
            newStage.setOnCloseRequest(e -> {
                try {
                    clientSocketTCP.close();
                    clientSocketUDP.close();
                } catch (IOException err) {
                }
                Platform.exit();
                System.exit(0);
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.out.println("Błąd przy połączeniu z serwerem");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
