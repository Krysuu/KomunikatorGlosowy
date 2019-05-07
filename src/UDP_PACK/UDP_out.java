package UDP_PACK;


import javax.sound.sampled.*;
import java.net.DatagramPacket;
import java.net.InetAddress;

import static GUI.Main.clientSocketUDP;
import static GUI.Main.ip;

public class UDP_out implements Runnable {
    private Boolean stopaudioCapture = false;
    private AudioFormat adFormat;
    private TargetDataLine targetDataLine;
    private Thread t;
    private String threadName = "udp_out";
    private byte tempBuffer[] = new byte[10000];

    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }

    private void connect_to_server() throws Exception {
        try {
            adFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
            targetDataLine.open(adFormat);
            targetDataLine.start();

            InetAddress IPAddress = InetAddress.getByName(ip);
            while (!stopaudioCapture) {
                int count = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
                if (count > 0) {
                    DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, IPAddress, 1240);
                    clientSocketUDP.send(sendPacket);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    @Override
    public void run() {
        try {
            connect_to_server();
        } catch (Exception e) {
        }
    }

    public void start() {
        System.out.println("Starting " + threadName);
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }

    public void close() {
        this.stopaudioCapture = true;
    }
}
