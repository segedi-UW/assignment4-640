import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
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
    protected DatagramSocket socket;
    protected boolean isSender;
    protected boolean connectionInitialized;
    protected InetAddress addr;

    protected Transport(int lp, int rp, String filename, int mtu, int sws) throws SocketException {
        this.lp = lp;
        this.rp = rp;
        this.filename = filename;
        this.mtu = mtu;
        this.sws = sws;
        buffer = new ArrayList<>();
        this.socket = new DatagramSocket(lp);
    }

    public boolean transfer() {
        // TODO Loop while we have bytes to send / rcv that have not been acked
        // each time loading the packet using the handlePacket method
        // that is implemented in the Sender and Receiver class


        // We need to print the following stats, which should be done here:
        // * <snd/rcv> <time> <flag-list> seq-number> <number of bytes> <ack number>

        TCPpacket p;

        // boolean toLoop = true;
        // while (toLoop) {
        //     // work loop
        //     if (p != null) handlePacket(p);
        //     else toLoop = false;
        //     return false; // FIXME tmp break;
        // }

        return false;
    }

    public void printPacket(TCPpacket p){
        // print stuff out here
        String msg = ""; // Can use a stringbuilder to make this faster
        if (isSender) msg += "snd ";
        else msg += "rcv ";
        msg += p.getTime();
        if(p.isSyn()) msg += " S";
        else msg += " -";
        if(p.isAck()) msg += " A";
        else msg += " -";
        if(p.isFin()) msg += " F";
        else msg += " -";
        if(p.getDataLen() > 0) msg += " D ";
        else msg += " - ";
        msg += p.getSeq() + " " + p.getDataLen() + " " + p.getAckNum();
        System.out.println(msg); 
    }

    public abstract TCPpacket handlePacket(TCPpacket p);
    public abstract TCPpacket getInitPacket();


    public static class Sender extends Transport {
        final private String rip; // remote ip
        // ArrayList buffer (protected)
        // constructor fields (protected)
        // Udp Socket (protected)

        public Sender(int lp, int rp, String rip, String filename, int mtu, int sws) throws SocketException {
            super(lp, rp, filename, mtu, sws);
            this.rip = rip;
            this.isSender = true;
            connectionInitialized = false;
            try {
                this.addr = InetAddress.getByName(rip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            testSend();
        }

        @Override
        public TCPpacket handlePacket(TCPpacket p) {
            return new TCPpacket(); // FIXME, take into account receieved packet p
        }

        @Override
        public TCPpacket getInitPacket() {
            // FIXME need to have specific init packet
            TCPpacket packet = new TCPpacket();
            packet.setSyn(true);
            packet.setSeq(3);
            packet.setTime(System.nanoTime());
            return packet;
        }

        public void testSend() {
            TCPpacket p = getInitPacket();
            try {
                socket.send(p.getPacket(addr, rp));
                printPacket(p);
                DatagramPacket pack = p.getPacket(addr, rp);
                TCPpacket newPack = new TCPpacket(pack.getData());
                printPacket(newPack);
                // DatagramPacket data = new DatagramPacket( new byte[ 64*1024 ], 64*1024 );
                // socket.receive(data);
                // TCPpacket ackPacket = new TCPpacket(data.getData());
                // printPacket(ackPacket);
            } catch (Exception e) {
                e.printStackTrace();
                //TODO: handle exception
            }
            // try {
            //     InetAddress addr = InetAddress.getByName(rip);
            //     byte[] send = "Hello World".getBytes( "UTF-8" );
            //     DatagramPacket data = new DatagramPacket(send, send.length, addr, rp);
            //     socket.send(data);
            // } catch (Exception e) {
            //     // TODO Auto-generated catch block
            //     e.printStackTrace();
            // } 
        }
    }

    public static class Receiver extends Transport {
        // ArrayList buffer (protected)
        // constructor fields (protected)
        // UDP Socket (protected)

        public Receiver(int lp, int rp, String filename, int mtu, int sws) throws SocketException {
            super(lp, rp, filename, mtu, sws);
            this.isSender = false;
            connectionInitialized = false;
            testRec();
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

        public void testRec() {
            try {
            DatagramPacket data = new DatagramPacket( new byte[ 64*1024 ], 64*1024 );
            socket.receive(data);
            TCPpacket prevPacket = new TCPpacket(data.getData());
            printPacket(prevPacket);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
    }
}
