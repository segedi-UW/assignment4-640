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

public class Receiver extends Transport {
	// ArrayList buffer (protected)
	// constructor fields (protected)
	// UDP Socket (protected)

	public Receiver(int lp, int rp, String filename, int mtu, int sws) throws SocketException {
		super(lp, rp, filename, mtu, sws);
		this.isSender = false; // FIXME why do we check this? in this class we know we are not?
		connectionInitialized = false;
		initConnection();
	}

	@Override
	public TCPpacket handlePacket(TCPpacket p) {
		return new TCPpacket(); // FIXME 
	}

	/**
	 * As per the documentation:
	 * 1) rcv the init with SYN control bit set 
	 * and initial sequence number x (ISN) 
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
	protected DatagramPacket initConnection() {
		try {
			DatagramPacket bufdp = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(bufdp);
			System.out.println("Received");
			TCPpacket init = TCPpacket.deserialize(bufdp.getData());

			TCPpacket initRsp = new TCPpacket();
			initRsp.setSyn(); 
			initRsp.setAck();
			initRsp.setSeq(0);
			initRsp.setAckNum(init.getSeq()+1);
			initRsp.setTime(init.getTime());

			sendData(bufdp, initRsp);
			System.out.println("Sent");

			TCPpacket rspAck = receiveData(bufdp, initRsp);
			if (!rspAck.isAck() && rspAck.getAckNum() != initRsp.getSeq()+1) {
				System.out.printf("Ack Expected (%d) != Actual (%d)\n", initRsp.getSeq()+1, rspAck.getAckNum());
				return null;
			}
			System.out.println("Received");
			System.out.println("Connection Initialized");
			return bufdp;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected void termConnection() {
		// FIXME
	}
}
