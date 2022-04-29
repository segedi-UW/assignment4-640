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
	final protected int lp; // local port
	final protected String filename;
	final protected int mtu;   // max transmission unit
	final protected int sws;    // sliding window size
	final protected List<Byte> buffer;
	final protected double a = .875;
	final protected double b = 1 - a;
	final protected int maxDataSize;

	protected int rp; // remote port
	private DatagramPacket bufferdp;
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
		this.maxDataSize = mtu - 20 - 8 - 24; // includes our header, used to split file into chunks
	}

	public boolean transfer() {
		// TODO Loop while we have bytes to send / rcv that have not been acked
		// each time loading the packet using the handlePacket method
		// that is implemented in the Sender and Receiver class
		if ((bufferdp = initConnection()) == null) {
			System.err.println("Failed to connect");
			return false;
		}

		rp = bufferdp.getPort();
		addr = bufferdp.getAddress();

		// We need to print the following stats, which should be done here:
		// * <snd/rcv> <time> <flag-list> seq-number> <number of bytes> <ack number>

		TCPpacket p;

		// This is the intermediate loop
		// boolean toLoop = true;
		// while (toLoop) {
		//     // work loop
		//     if (p != null) handlePacket(p);
		//     else toLoop = false;
		//     return false; // FIXME tmp break;
		// }

		termConnection();
		return true;
	}

	public void printPacket(TCPpacket p, boolean isSending){
		// print stuff out here
		String msg = ""; // Can use a stringbuilder to make this faster
		if (isSending) msg += "snd ";
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

	public void sendData(DatagramPacket indp, TCPpacket p) {
		if (indp == null)
			throw new NullPointerException("buffer DatagramPacket is not initialized. Likely called sendData(TCPpacket) before or in initConnection()");
		indp.setData(p.serialize());
		try {
			socket.send(indp);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1); // FIXME may change to return boolean etc
		}
	}

	public void sendData(TCPpacket p) {
		if (bufferdp == null)
			throw new NullPointerException("Channel is not init");
		sendData(bufferdp, p);
	}

	/**
	 * NOTE Should only be called after initConnection returns
	 *
	 */
	public TCPpacket receiveData(TCPpacket out) {
		return receiveData(bufferdp, out);
	}

	public TCPpacket receiveData(DatagramPacket indp, TCPpacket out) {
		if (indp == null) throw new NullPointerException("buffer DatagramPacket is not initialized. Likely called receiveData(TCPpacket) before or in initConnection()");
		int reTransmissions = 0;
		try {
			socket.setSoTimeout(getTimeOut());
		} catch (Exception e) {
			e.printStackTrace();
		}
		// set bufferdp
		indp.setData(out.serialize());
		while(reTransmissions < 16) {
			try {
				socket.receive(indp);
				TCPpacket p = TCPpacket.deserialize(indp.getData());
				updateTimeOut(p);
				return p;
			} catch (SocketTimeoutException e) {
				// resend
				try {
					System.out.println("Retransmitting");
					socket.send(bufferdp); // should be set with data already
					reTransmissions += 1;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				continue;
			}
			catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			}
		}
		System.out.println("Tried Retransmitting 16 times");
		System.exit(1);
		return null;
	}

	public abstract TCPpacket handlePacket(TCPpacket p);

	/**
	 * The connection should be initialized on return
	 * if the init is successful, performing
	 * the respective half of the three-way handshake.
	 *
	 * @return the response Datagram with appropriate
	 * set size for reuse.
	 */
	protected abstract DatagramPacket initConnection();

	/**
	 * After this returns the connection should
	 * be terminated. Perform termination
	 * procedure, then close sockets.
	 */
	protected abstract void termConnection();

}
