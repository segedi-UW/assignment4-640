import java.net.DatagramPacket;
import java.net.SocketException;


public class TCPend {


    private static String getExpects() {
        final String sexp = "Send: java TCPend -p <port> -s <remote ip> -a <remote port> -f <filename> -m <mtu> -c <sws>\n";
        final String rexp = "Receive: java TCPend -p <port> -m <mtu> -c <sws> -f <filename>";
        return "Expects arguments:\n" + sexp + rexp;
    }

    public static void main(String[] args) throws SocketException {

        if (args.length <= 0) System.out.println(getExpects());
        TransportBuilder tbuilder = new TransportBuilder();

        String popt = null;
        for (String arg : args) {
            if (arg.startsWith("-")) {
                popt = arg.substring(1);
            } else if (popt != null && !tbuilder.add(popt, arg)) {
                System.err.println("Error building Transport, bad args: " + popt + ", " + arg);
                return;
            }
        }

        Transport t = tbuilder.build();
        
        // if (!t.transfer()) {
        //     System.err.println("Error sending all bytes");
        // }

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

        Transport build() throws SocketException {
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
