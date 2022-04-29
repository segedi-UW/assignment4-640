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
		//testSend();
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

	@Override
	public TCPpacket getInitPacket() {
		// FIXME need to have specific init packet
		TCPpacket packet = new TCPpacket();
		packet.setSyn();
		packet.setSeq(3);
		return packet;
	}

	private void bufferWindow() throws IOException {
		bufn = Math.min(reader.available(), buf.length);

		for (int i = 0; i < bufn; i++)
			buf[i] = (byte) reader.read();

		nextBufSeq += sws;
	}

	public void testSend() {
		TCPpacket p = getInitPacket();

		try {
			int rd = 0;
			byte[] testbuf = new byte[reader.available()];
			for (int i = 0; reader.available() > 0; i++, rd++)
				testbuf[i] = (byte) reader.read();
			System.out.println("Sending " + testbuf.length + " bytes");

			ByteBuffer bb = ByteBuffer.wrap(testbuf);
			StringBuffer sb = new StringBuffer("Begginning Should match:\n");
			while (bb.remaining() > 1)
				sb.append(bb.get()).append(' ');
			System.out.println(sb.toString());

			p.setData(testbuf, 0, rd);
			DatagramPacket pack = p.getPacket(addr, rp);
			socket.send(pack);
			printPacket(p, true);
			TCPpacket newPack = TCPpacket.deserialize(pack.getData());
			printPacket(newPack, false);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			//TODO: handle exception
		} 
	}

	private void initConnection() {
		TCPpacket p = getInitPacket();
		try {
			p.setCurrentTime();
			DatagramPacket d = p.getPacket(addr, rp);
			// System.out.println("Sending packet:");
			// TCPpacket.printPacket(d.getData());
			socket.send(d);
			printPacket(TCPpacket.deserialize(d.getData()), true);
			DatagramPacket data = new DatagramPacket( new byte[ mtu ], mtu );
			TCPpacket prevPacket = receiveData(data, d);
			socket.receive(data);
			printPacket(prevPacket, false);

			TCPpacket packet = new TCPpacket();
			packet.setAck();
			packet.setAckNum(prevPacket.getSeq()+1);

			packet.setCurrentTime();
			d = packet.getPacket(addr, rp);
			System.out.println("Sending packet2:");
			// TCPpacket.printPacket(d.getData());
			socket.send(d);
			printPacket(TCPpacket.deserialize(d.getData()), true);
		}
		catch (Exception e){
			e.printStackTrace();
			System.exit(1);
		}
	}
}
