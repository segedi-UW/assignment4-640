import java.net.DatagramPacket;

public class TCPend {


    private static String getExpects() {
        final String sexp = "java TCPend -p <port> -s <remote ip> -a <remote port> -f <filename> -m <mtu> -c <sws>\n";
        final String rexp = "java TCPend -p <port> -m <mtu> -c <sws> -f <filename>";
        return "Expects arguments:\n" + sexp + rexp;
    }

    public static void main(String[] args) {

        if (args.length <= 0) System.out.println(getExpects());
        TransportBuilder tbuilder = new TransportBuilder();

        String popt = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                popt = arg.substring(1);
                continue;
            } else if (popt != null && !tbuilder.add(popt, arg)) {
                System.err.println("Error building Transport.");
                return;
            }
        }

        Transport t = tbuilder.build();
        if (!t.send()) {
            System.err.println("Error sending all bytes");
        }

    }

    /**
     *  The Transport class handles the bulk of the TCP general
     *  processing. Specifics are implemented via the handlePacket(DatagramPacket)
     *  method of the implementing class.
     */
    private static abstract class Transport {
        final protected int lp, rp; // local port, remote port
        final protected String filename;
        final protected int mtu;   // max transmission unit
        final protected int sws;    // sliding window size
        // TODO create packet buffer

        protected Transport(int lp, int rp, String filename, int mtu, int sws) {
            this.lp = lp;
            this.rp = rp;
            this.filename = filename;
            this.mtu = mtu;
            this.sws = sws;
        }

        public boolean send() {
            // TODO Loop while we have bytes to send / rcv that have not been acked
            // each time loading the packet using the handlePacket method
            // that is implemented in the Sender and Receiver class
            // 
            // We need to print the following stats, which should be done here:
            // * <snd/rcv> <time> <flag-list> seq-number> <number of bytes> <ack number>

            DatagramPacket p = getInitPacket();
            while (true) {
                // work loop
                if (p != null) handlePacket(p);
                return false; // FIXME tmp break;
            }

            //return false;
        }

        // TODO may return something other than int if needed
        public abstract DatagramPacket handlePacket(DatagramPacket p);
        public abstract DatagramPacket getInitPacket();

    }

    private static class Sender extends Transport {
        final private String rip; // remote ip

        public Sender(int lp, int rp, String rip, String filename, int mtu, int sws) {
            super(lp, rp, filename, mtu, sws);
            this.rip = rip;
        }

        @Override
        public DatagramPacket handlePacket(DatagramPacket p) {
            return null; // FIXME should not be null
        }

        @Override
        public DatagramPacket getInitPacket() {
            // FIXME need to have specific init packet
            return new DatagramPacket(new byte[0], 0);
        }
    }

    private static class Receiver extends Transport {
        public Receiver(int lp, int rp, String filename, int mtu, int sws) {
            super(lp, rp, filename, mtu, sws);
        }

        @Override
        public DatagramPacket handlePacket(DatagramPacket p) {
            return null; // should be null as we do not init as receiver
        }

        @Override
        public DatagramPacket getInitPacket() {
            // FIXME need to have specific init packet
            return new DatagramPacket(new byte[0], 0);
        }
    }

    /**
     * This class creates the coresponding Transport with respect to the arguments
     * passed into it via the add(String, String) method.
     *
     * @see add(String, String)
     */
    private static class TransportBuilder {
        private static int parseIntFailure = -1;

        private int lp, rp; // local port, remote port
        private String rip; // remote ip
        private String filename;
        private int mtu;   // max transmission unit
        private int sws;    // sliding window size

        Transport build() {
            return isSender() ? new Sender(lp, rp, rip, filename, mtu, sws) :
                new Receiver(lp, rp, filename, mtu, sws);
        }

        public boolean isSender() {
            return rip != null;
        }

        /**
         *  @param opt flag char
         *  @param arg passed arg for the flag
         */
        public boolean add(String opt, String arg) {
            switch (opt) {
                case "p":
                    lp = tryParseInt(arg);
                    if (lp == parseIntFailure) return false;
                    break;
                case "s":
                    rip = arg;
                    break;
                case "a":
                    rp = tryParseInt(arg);
                    if (rp == parseIntFailure) return false;
                    break;
                case "f":
                    filename = arg;
                    break;
                case "m":
                    mtu = tryParseInt(arg);
                    if (mtu == parseIntFailure) return false;
                    break;
                case "c":
                    sws = tryParseInt(arg);
                    if (sws == parseIntFailure) return false;
                    break;
                default:
                    System.err.println("Unexpected option: " + opt + "\n" + getExpects());
                    return false;
            }
            return true;
        }

        private int tryParseInt(String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException e) {
                System.err.println(str + " is not an integer");
                return parseIntFailure;
            }
        }
    }
}
