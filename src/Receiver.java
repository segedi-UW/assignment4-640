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

	private TCPpacket getInitPacket() {
		TCPpacket packet = new TCPpacket();
		packet.setSyn();
		packet.setSeq(0);
		return packet;
		// return null; // should be null as we do not init as receiver
	}

	@Override
	protected boolean initConnection() {
		try {
			DatagramPacket data = new DatagramPacket( new byte[ mtu ], mtu );
			socket.receive(data);
			System.out.println("Received");
			TCPpacket prevPacket = TCPpacket.deserialize(data.getData());
			//printPacket(prevPacket, false);

			TCPpacket packet = getInitPacket();
			packet.setAck();
			packet.setAckNum(prevPacket.getSeq()+1);
			packet.setTime(prevPacket.getTime());

			DatagramPacket pack = packet.getPacket(data.getAddress(), data.getPort());
			//printPacket(TCPpacket.deserialize(pack.getData()), true);
			socket.send(pack);
			System.out.println("Sent");

			data = new DatagramPacket( new byte[ mtu ], mtu );
			prevPacket = receiveData(data, pack);
			System.out.println("Received");
			//printPacket(prevPacket, false);
			// prevPacket = TCPpacket.deserialize(data.getData());
			System.out.println("Connection Initialized");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
