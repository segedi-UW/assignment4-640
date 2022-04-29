import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class reciever {
    public static void main(String[] args) {
        try {
        int lp = 5000;
        DatagramSocket sock = new DatagramSocket(lp);
        String word = "hello world";
        byte[] buf = word.getBytes();
        DatagramPacket data = new DatagramPacket(buf, buf.length);
        sock.receive(data);
        word = "nice avery";
        buf = word.getBytes();
        System.out.println("INET:" + data.getAddress());
        data = new DatagramPacket(buf, buf.length, data.getAddress(), data.getPort());
        sock.send(data);
        System.out.println("Sent: " + word);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
