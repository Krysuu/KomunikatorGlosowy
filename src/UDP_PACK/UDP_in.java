package UDP_PACK;


import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.DatagramPacket;

import static GUI.Main.clientSocketUDP;


public class UDP_in implements Runnable {
    private AudioFormat adFormat;
    private AudioInputStream audioInputStream;
    private SourceDataLine sourceDataLine;

    private byte receiveData[] = new byte[10000];
    private byte tempBuffer[] = new byte[10000];
    private boolean on = true;
    private Thread t;
    private String threadName = "udp_in";
    private AudioFormat getAudioFormat() {
        float sampleRate = 16000.0F;
        int sampleInbits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleInbits, channels, signed, bigEndian);
    }

    public void connect_to_server() throws Exception {
        try {
            adFormat = getAudioFormat();
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
            sourceDataLine  = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
            sourceDataLine.open(adFormat);
            sourceDataLine.start();
            while (on) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                clientSocketUDP.receive(receivePacket);
                try {
                    byte audioData[] = receivePacket.getData();
                    InputStream byteInputStream = new ByteArrayInputStream(audioData);
                    audioInputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());
                    int count;
                    count = audioInputStream.read(tempBuffer, 0, tempBuffer.length);
                    sourceDataLine.write(tempBuffer, 0, count);
                } catch (Exception e) {
                    System.out.println(e);
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
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
        this.on = false;

    }
}
