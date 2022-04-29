import java.util.List;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

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
	final protected double a = .875;
	final protected double b = 1 - a;

	protected long currentAck;
	protected DatagramSocket socket;
	protected boolean isSender;
	protected boolean connectionInitialized;
	protected InetAddress addr;
	protected long timeOut = 5000;
	protected double ERTT;
	protected double EDEV;

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

	public void printPacket(TCPpacket p, boolean sending){
		// print stuff out here
		String msg = ""; // Can use a stringbuilder to make this faster
		if (sending) msg += "snd ";
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

	private void updateTimeOut(TCPpacket p) {
		long S = p.getSeq();
		long T = p.getTime();
		long C = System.nanoTime();
		if (S == 0){
			ERTT = (C - T);
			EDEV = 0;
			timeOut = (long) (2*ERTT);
		}
		else{
			double SRTT = (C - T);
			double SDEV = Math.abs(SRTT - ERTT);
			ERTT = a*ERTT + (1-a)*SRTT;
			EDEV = b*EDEV + (1-b)*SDEV;
			timeOut = (long) (ERTT + 4*EDEV);
		}
	}

	private int getTimeOut() {
		return (int) this.timeOut;
	}

	public TCPpacket receiveData(DatagramPacket data, DatagramPacket out) {
		int reTransmissions = 0;
		try {
			socket.setSoTimeout(getTimeOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
		while(reTransmissions < 16) {
			try {
				socket.receive(data);
				TCPpacket p = TCPpacket.deserialize(data.getData());
				updateTimeOut(p);
				return p;
			} catch (SocketTimeoutException e) {
				// resend
				try {
					System.out.println("Retransmitting");
					socket.send(out);
					reTransmissions += 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				continue;
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("Tried Retransmitting 16 times");
		System.exit(1);
		return null;
	}

	public abstract TCPpacket handlePacket(TCPpacket p);
	public abstract TCPpacket getInitPacket();

}
