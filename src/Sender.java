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

	private void bufferWindow() throws IOException {
		bufn = Math.min(reader.available(), buf.length);

		for (int i = 0; i < bufn; i++)
			buf[i] = (byte) reader.read();

		nextBufSeq += sws;
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
			TCPpacket init = new TCPpacket();
			init.setSyn();
			init.setSeq(3); // FIXME random within reason - look up details
			init.setCurrentTime();
			DatagramPacket bufdp = new DatagramPacket(new byte[mtu], mtu, addr, rp);

			sendData(bufdp, init);

			System.out.println("Reading");
			TCPpacket initRsp = receiveData(bufdp, init);
			System.out.println("Received packet");
			if (!initRsp.isSyn() || !initRsp.isAck()) {
				System.err.println("Expected SYN and ACK to be set");
				return null;
			} else if (initRsp.getAckNum() != init.getSeq()+1) {
				System.err.printf("Ack Expected (%d) Actual (%d)\n", init.getSeq()+1, initRsp.getAckNum());
				return null;
			}

			TCPpacket rspAck = new TCPpacket();
			rspAck.setAck();
			rspAck.setAckNum(initRsp.getSeq()+1);
			rspAck.setCurrentTime();
			sendData(bufdp, rspAck);
			System.out.println("Sent Acknowledgement");
			return bufdp;
		}
		catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	@Override
	protected TCPpacket transferData() {
		return null;
	}

	@Override
	protected void termConnection(TCPpacket finPacket) {
		// FIXME
	}
}
