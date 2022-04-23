import java.util.List;
import java.util.ArrayList;

/**
 *  The Transport class handles the bulk of the TCP general
 *  processing. Specifics are implemented via the handlePacket(DatagramPacket)
 *  method of the implementing class.
 */
public abstract class Transport {
    final protected int lp, rp; // local port, remote port
    final protected String filename;
    final protected int mtu;   // max transmission unit
    final protected int sws;    // sliding window size
    final protected List<Byte> buffer;

    protected long currentAck;
    protected DatagramSocket;

    protected Transport(int lp, int rp, String filename, int mtu, int sws) {
        this.lp = lp;
        this.rp = rp;
        this.filename = filename;
        this.mtu = mtu;
        this.sws = sws;
        buffer = new ArrayList<>(); 
    }

    public boolean transfer() {
        // TODO Loop while we have bytes to send / rcv that have not been acked
        // each time loading the packet using the handlePacket method
        // that is implemented in the Sender and Receiver class


        // We need to print the following stats, which should be done here:
        // * <snd/rcv> <time> <flag-list> seq-number> <number of bytes> <ack number>

        TCPpacket p = getInitPacket();
        while (true) {
            // work loop
            if (p != null) handlePacket(p);
            return false; // FIXME tmp break;
        }

        // print stuff out here
        //return false;
    }

    public abstract TCPpacket handlePacket(TCPpacket p);
    public abstract TCPpacket getInitPacket();


    public static class Sender extends Transport {
        final private String rip; // remote ip
        // ArrayList buffer (protected)
        // constructor fields (protected)

        public Sender(int lp, int rp, String rip, String filename, int mtu, int sws) {
            super(lp, rp, filename, mtu, sws);
            this.rip = rip;
        }

        @Override
        public TCPpacket handlePacket(TCPpacket p) {
            return new TCPpacket(); // FIXME, take into account receieved packet p
        }

        @Override
        public TCPpacket getInitPacket() {
            // FIXME need to have specific init packet
            return new TCPpacket();
        }
    }

    public static class Receiver extends Transport {
        // ArrayList buffer (protected)
        // constructor fields (protected)

        public Receiver(int lp, int rp, String filename, int mtu, int sws) {
            super(lp, rp, filename, mtu, sws);
            // read the file given by filename -> buffer
        }

        @Override
        public TCPpacket handlePacket(TCPpacket p) {
            return new TCPpacket(); // FIXME 
        }

        @Override
        public TCPpacket getInitPacket() {
            return null; // should be null as we do not init as receiver
        }
    }
}
