import java.util.List;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
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

	protected long currentAck;
	protected DatagramSocket socket;
	protected boolean isSender;
	protected boolean connectionInitialized;
	protected InetAddress addr;

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

	public void printPacket(TCPpacket p){
		// print stuff out here
		String msg = ""; // Can use a stringbuilder to make this faster
		if (this instanceof Sender) msg += "snd ";
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

	public abstract TCPpacket handlePacket(TCPpacket p);
	public abstract TCPpacket getInitPacket();


	public static class Sender extends Transport {
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
			testSend();
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
				byte[] testbuf = new byte[256];
				for (int i = 0; reader.available() > 0; i++, rd++)
					testbuf[i] = (byte) reader.read();
				System.out.println("Sending " + testbuf.length + " bytes");

				ByteBuffer bb = ByteBuffer.wrap(testbuf);
				StringBuffer sb = new StringBuffer("Should match:\n");
				while (bb.remaining() > 1)
					sb.append(bb.getChar());
				System.out.println(sb.toString());

				p.setData(testbuf, 0, rd);
				DatagramPacket pack = p.getPacket(addr, rp);
				socket.send(pack);
				printPacket(p);
				TCPpacket newPack = TCPpacket.deserialize(pack.getData());
				printPacket(newPack);
				// DatagramPacket data = new DatagramPacket( new byte[ 64*1024 ], 64*1024 );
				// socket.receive(data);
				// TCPpacket ackPacket = new TCPpacket(data.getData());
				// printPacket(ackPacket);
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
				//TODO: handle exception
			} 
			// try {
			//     InetAddress addr = InetAddress.getByName(rip);
			//     byte[] send = "Hello World".getBytes( "UTF-8" );
			//     DatagramPacket data = new DatagramPacket(send, send.length, addr, rp);
			//     socket.send(data);
			// } catch (Exception e) {
			//     // TODO Auto-generated catch block
			//     e.printStackTrace();
			// } 
		}
	}

	public static class Receiver extends Transport {
		// ArrayList buffer (protected)
		// constructor fields (protected)
		// UDP Socket (protected)

		public Receiver(int lp, int rp, String filename, int mtu, int sws) throws SocketException {
			super(lp, rp, filename, mtu, sws);
			this.isSender = false; // FIXME why do we check this? in this class we know we are not?
			connectionInitialized = false;
			testRec();
		}

		@Override
		public TCPpacket handlePacket(TCPpacket p) {
			return new TCPpacket(); // FIXME 
		}

		@Override
		public TCPpacket getInitPacket() {
			return null; // should be null as we do not init as receiver
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
					sb.append(bb.getChar());

				System.out.println(sb.toString());
			} catch (Exception e) {
				System.err.println(e.getMessage());
				e.printStackTrace();
			} 
		}
	}
}
