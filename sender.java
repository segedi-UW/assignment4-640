import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.xml.crypto.Data;

public class sender {
    public static void main(String[] args) {
        try {
            
        int lp = 3333;
        DatagramSocket sock = new DatagramSocket(lp);
        String word = "hello world";
        byte[] buf = word.getBytes();
        DatagramPacket data = new DatagramPacket(buf, buf.length, InetAddress.getByName("localhost"), 5000);
        sock.send(data);
        sock.receive(data);
        String s = new String(data.getData());
        System.out.println("Recieved Back: " + s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
