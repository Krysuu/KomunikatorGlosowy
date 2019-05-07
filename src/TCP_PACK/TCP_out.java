package TCP_PACK;

import java.io.*;

import static GUI.Main.*;
import static GUI.Main.clientSocketUDP;
import static GUI.Main.udp_in;

public class TCP_out implements Runnable {

    private Thread t;
    private boolean on = true;
    private volatile String wiadomosc_wysylana = "";

    public void setOn(boolean on) {
        this.on = on;
    }

    public void setWiadomosc_wysylana(String wiadomosc_wysylana) {
        this.wiadomosc_wysylana = wiadomosc_wysylana;
    }

    public void connect_to_server() throws Exception {

        DataOutputStream outToServer = new DataOutputStream(clientSocketTCP.getOutputStream());
        wiadomosc_wysylana = String.valueOf((char) 4) + "NCK" + nick + "\n";
        outToServer.writeBytes(wiadomosc_wysylana);
        System.out.println(wiadomosc_wysylana);
        wiadomosc_wysylana = "";
        while (on) {
            synchronized (monitor) {
                if (!wiadomosc_wysylana.equals("")) {
                    wiadomosc_wysylana = wiadomosc_wysylana + "\n";
                    outToServer.writeBytes(wiadomosc_wysylana);
                    System.out.println(wiadomosc_wysylana);
                    wiadomosc_wysylana = "";
                } else {
                    monitor.wait();
                }
            }
        }
        udp_out.close();
        udp_in.close();
        clientSocketUDP.close();
        System.out.println("out_off");
    }

    @Override
    public void run() {
        try {
            connect_to_server();
        } catch (Exception e) {
        }
    }

    public void start() {
        System.out.println("Starting tcp_out");
        if (t == null) {
            t = new Thread(this, "tcp_out");
            t.start();
        }
    }
}