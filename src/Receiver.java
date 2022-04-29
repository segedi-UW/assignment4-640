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
		//testRec();
		initConnection();
	}

	@Override
	public TCPpacket handlePacket(TCPpacket p) {
		return new TCPpacket(); // FIXME 
	}

	@Override
	public TCPpacket getInitPacket() {
		TCPpacket packet = new TCPpacket();
		packet.setSyn();
		packet.setSeq(3);
		return packet;
		// return null; // should be null as we do not init as receiver
	}

	public void testRec() {
		try {
			DatagramPacket data = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(data);
			TCPpacket prevPacket = TCPpacket.deserialize(data.getData());
			System.out.println("RCVL: " + prevPacket.getDataLen());
			printPacket(prevPacket);

			ByteBuffer bb = ByteBuffer.wrap(prevPacket.getData());
			StringBuffer sb = new StringBuffer("Packet msg:\n");

			while (bb.remaining() > 1) // in java chars are length 2
				sb.append(bb.get()).append(' ');

			System.out.println(sb.toString());
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		} 
	}

	private void initConnection() {
		try {
			DatagramPacket data = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(data);
			System.out.println("Recieved");
			TCPpacket prevPacket = TCPpacket.deserialize(data.getData());
			System.out.println(prevPacket);

			TCPpacket packet = getInitPacket();
			// packet.setAck();
			// packet.setSyn();
			// packet.setAckNum(prevPacket.getSeq()+1);
			// packet.setSeq(100); // Might need to change to random number
			// packet.setCurrentTime();
			// printPacket(packet);

			DatagramPacket pack = packet.getPacket(data.getAddress(), data.getPort());
			socket.send(pack);
			printPacket(TCPpacket.deserialize(data.getData()));

			data = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(data);
			prevPacket = TCPpacket.deserialize(data.getData());
			System.out.println("Connection Initialized");

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
