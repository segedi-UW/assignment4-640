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

public class Sender extends Transport {
	final private String rip; // remote ip
	private FileInputStream reader;
	private byte[] buf;
	private int bufn;
	private int nextBufSeq;
	// ArrayList buffer (protected)
	// constructor fields (protected)
	// Udp Socket (protected)

	public Sender(int lp, int rp, String rip, String filename, int mtu, int sws) throws SocketException {
		super(lp, rp, filename, mtu, sws);
		this.rip = rip;
		this.isSender = true;
		connectionInitialized = false;
		try {
			System.out.println("RIP:" + rip + ":");
			this.addr = InetAddress.getByName(rip);
			buf = new byte[sws];
			bufn = 0;
			reader = new FileInputStream(filename);
		} catch (UnknownHostException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		} 
		initConnection();
	}

	@Override
	public TCPpacket handlePacket(TCPpacket p) {
		// read the file given by filename -> buffer
		// buffer the next sws number of bytes if 
		try {
			if (p.getSeq() == nextBufSeq && reader.available() > 0)
				bufferWindow();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
		return new TCPpacket(); // FIXME, take into account receieved packet p
	}

	private TCPpacket getInitPacket() {
		TCPpacket packet = new TCPpacket();
		packet.setSyn();
		packet.setSeq(3); // FIXME random within reason - look up details
		packet.setCurrentTime();
		return packet;
	}

	private void bufferWindow() throws IOException {
		bufn = Math.min(reader.available(), buf.length);

		for (int i = 0; i < bufn; i++)
			buf[i] = (byte) reader.read();

		nextBufSeq += sws;
	}

	/**
	 * As per the documentation:
	 * 1) init the connection by sending TCP segment
	 * with SYN control bit set and initial sequence
	 * number x (ISN) 
	 *
	 *    A		| 	  B
	 *   SYN    |  SYN ACK
	 *  ISN=x   |   ISN=y
	 *
	 * 2) Read, should have SYN set and independent
	 * ISN; Should have ACK set to indicate the next
	 * expected byte should contain data with start
	 * number x + 1
	 *
	 * 3) After reading B's ISN and ACK, sends
	 * final acknowledment segment to B, setting
	 * the ACK control bit and setting ackNum to y+1,
	 * indicating the next byte expected is y+1
	 */
	@Override
	protected boolean initConnection() {
		try {
			TCPpacket p = getInitPacket();
			DatagramPacket d = p.getPacket(addr, rp);

			socket.send(d);
			System.out.println("Sent");
			//printPacket(TCPpacket.deserialize(d.getData()), true);
			DatagramPacket data = new DatagramPacket( new byte[ mtu ], mtu );
			//System.out.println("Reading");
			System.out.println("Reading");
			TCPpacket prevPacket = receiveData(data, d);
			System.out.println("Received packet");
			//socket.receive(data);
			//printPacket(prevPacket, false);

			TCPpacket packet = new TCPpacket();
			packet.setAck();
			packet.setAckNum(prevPacket.getSeq()+1);

			packet.setCurrentTime();
			d = packet.getPacket(addr, rp);
			// TCPpacket.printPacket(d.getData());
			socket.send(d);
			System.out.println("Sent last");
			//printPacket(TCPpacket.deserialize(d.getData()), true);
		}
		catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
