package TCP_PACK;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Observable;

import static GUI.Main.*;

public class TCP_in extends Observable implements Runnable {

    private Thread t;
    boolean on = true;
    public void setOn(boolean on) {
        this.on = on;
    }
    public void connect_to_server() throws Exception {
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocketTCP.getInputStream()));
        String text;
        while (on) {
            text = inFromServer.readLine();
            if (text == null) {
                System.out.println("Serwer zakończył połączenie");
                pol_out.setOn(false);
                on = false;
            } else {
                controller.rText(text);
            }
        }

        udp_out.close();
        udp_in.close();
        clientSocketUDP.close();
        System.out.println("in_off");

    }

    @Override
    public void run() {
        try {
            connect_to_server();
        } catch (Exception e) {
        }
    }

    public void start() {
        System.out.println("Starting tcp_in");
        if (t == null) {
            t = new Thread(this, "tcp_in");
            t.start();
        }
    }
}