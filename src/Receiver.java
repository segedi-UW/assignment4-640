import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;

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
	}

	/*
	 * A            			B
	 * |						|
	 * | SYN seq=x ->			|
	 * |						|
	 * |   <- SYN seq=y ACK=x+1	| Set ACK and set ackNum to x+1
	 * | 						|
	 * | ACK=y+1 ->				|
	 * |						|
	 */
	@Override
	protected DatagramPacket initConnection() {
		try {
			DatagramPacket bufdp = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(bufdp);
			TCPpacket init = TCPpacket.deserialize(bufdp.getData());
			printPacket(init, false);

			TCPpacket initRsp = new TCPpacket();
			initRsp.setSyn(); 
			initRsp.setAck();
			initRsp.setSeq(0);
			initRsp.setAckNum(init.getSeq()+1);
			initRsp.setTime(init.getTime());

			sendData(bufdp, initRsp);
			// System.out.println("Sent");

			TCPpacket rspAck = receiveData(bufdp, initRsp);
			if (!rspAck.isAck() && rspAck.getAckNum() != initRsp.getSeq()+1) {
				System.out.printf("Ack Expected (%d) != Actual (%d)\n", initRsp.getSeq()+1, rspAck.getAckNum());
				return null;
			}
			// System.out.println("Received");
			System.out.println("Connection Initialized");
			return bufdp;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private TCPpacket handlePacket(TCPpacket p) {
		if (p.getAckNum() > currentAck + maxDataSize * (buffer.length - 1))
			return null; // outside of window
		if (p.isFin()) return p;
		int bi = p.getAckNum() / (maxDataSize + currentAck);


		return null;
	}

	private TCPpacket readAll() {
		ByteBuffer buf = ByteBuffer.allocate(maxDataSize + TCPpacket.HEADERN);
		TCPpacket p;
		int rc = 0;
		try {
			channel.configureBlocking(false);
		} catch (IOException e) {
			System.err.println("Failed to configure to non-blocking channel");
			System.exit(1);
		}
		try {
			while ((rc = channel.read(buf)) > 0) {
				try {
					p = TCPpacket.deserialize(buf.array());
					if (p.isFin()) return p;
					handlePacket(p);
				} catch (ChecksumException e) {
					continue;
				}
				buf.rewind();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			throw new IllegalStateException("Failed to read channel");
		} finally {
			try {
				channel.configureBlocking(true);
			} catch (IOException e) {
				System.err.println("Failed to reconfigure to blocking channel");
				System.exit(1);
			}
		}
		if (rc < 0)
			throw new IllegalStateException("Socket was closed");
		return null;
	}

	protected TCPpacket transferData() {
		DatagramPacket buf = new DatagramPacket(new byte[mtu], mtu);
		while(true){
			try {
				socket.receive(buf);
				TCPpacket p = TCPpacket.deserialize(buf.getData());
				printPacket(p, false);
			} catch (Exception e) {
				//TODO: handle exception
			}
		}
		// TCPpacket rcv = null;
		// try (FileOutputStream out = new FileOutputStream(filename, true)) {
		// 	TCPpacket lastAck = new TCPpacket();
		// 	TCPpacket fin = null;
		// 	lastAck.setAckNum(currentAck);
		// 	while (!(rcv = receiveData(lastAck)).isFin()) { // while not fin packet
		// 		fin = handlePacket(rcv);
		// 		if (fin != null) return fin;
		// 		fin = readAll(); // proceses all until fin packet
		// 		if (fin != null) return fin;
		// 	}
		// } catch (IOException e) {
		// 	System.err.println("Failed to write to file: " + e.getMessage());
		// }

		// if (!rcv.isFin())
		// 	throw new IllegalStateException("Terminated before fin packet!");

		// return rcv;
	}

	@Override
	protected void termConnection(TCPpacket finPacket) {
		// FIXME
	}
}
